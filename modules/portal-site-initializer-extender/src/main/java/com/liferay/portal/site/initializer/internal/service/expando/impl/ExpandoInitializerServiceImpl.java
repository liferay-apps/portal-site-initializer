package com.liferay.portal.site.initializer.internal.service.expando.impl;

import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.expando.kernel.model.ExpandoColumn;
import com.liferay.expando.kernel.model.ExpandoTable;
import com.liferay.expando.kernel.service.ExpandoColumnLocalService;
import com.liferay.expando.kernel.service.ExpandoTableLocalService;
import com.liferay.expando.kernel.util.ExpandoBridgeFactoryUtil;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.*;
import com.liferay.portal.site.initializer.extender.context.InitializerContext;
import com.liferay.portal.site.initializer.extender.model.InitializerStrategy;
import com.liferay.portal.site.initializer.extender.util.ExpandoKeys;
import com.liferay.portal.site.initializer.internal.service.expando.ExpandoInitializerService;
import com.liferay.portal.site.initializer.internal.util.SiteInitializerUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component(service = ExpandoInitializerService.class)
public class ExpandoInitializerServiceImpl implements ExpandoInitializerService {

    public static final String DESCRIPTOR_PATH = "/site-initializer/expando-columns.json";
    private static final String[] PRESERVED_CUSTOM_FIELDS = new String[]{
            ExpandoKeys.ACCESS_TOKEN,
            ExpandoKeys.ACCESS_SECRET,
            ExpandoKeys.REQUEST_TOKEN,
            ExpandoKeys.REQUEST_SECRET,
            ExpandoKeys.FILE_NAME,
            ExpandoKeys.SI_KEY,
            ExpandoKeys.SI_STRATEGY
    };

    @Override
    public void initializeCustomFields(InitializerContext context) throws Exception {

        ServletContext servletContext = context.getServletContext();

        String json = SiteInitializerUtil.read(DESCRIPTOR_PATH, servletContext);
        if (Validator.isNull(json)) {
            return;
        }

        InitializerStrategy strategy = context.getStrategy();

        ServiceContext serviceContext = context.getServiceContext();
        long companyId = serviceContext.getCompanyId();

        List<Long> processedExpandoColumnIds = new ArrayList<>();

        JSONArray jsonArray = JSONFactoryUtil.createJSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            String modelResource = jsonObject.getString("modelResource");
            ExpandoBridge expandoBridge = ExpandoBridgeFactoryUtil.getExpandoBridge(companyId, modelResource);
            if (expandoBridge == null) {
                _log.warn(String.format("ExpandoBridge not found for companyId=%d and resource='%s'.", companyId, modelResource));
                continue;
            }

            String attributeName = jsonObject.getString("name");
            int dataType = jsonObject.getInt("dataType");
            UnicodeProperties unicodeProperties = getUnicodeProperties(jsonObject);

            Serializable expandoAttribute = expandoBridge.getAttribute(attributeName, false);
            if (expandoAttribute == null) {

                // Create Custom Field

                expandoBridge.addAttribute(attributeName, dataType, false);
                if (MapUtil.isNotEmpty(unicodeProperties)) {
                    expandoBridge.setAttributeProperties(attributeName, unicodeProperties, false);
                }
                _log.info(String.format("Created custom field '%s' on model '%s'.", attributeName, modelResource));

            } else {

                if (!InitializerStrategy.CREATE.equals(strategy)) {

                    // Update Custom Field - if update is required, and if not 'CREATE' strategy

                    ExpandoColumn expandoColumn = getExpandoColumn(companyId, modelResource, attributeName);

                    boolean changedType = expandoColumn.getType() != dataType;
                    boolean changedProperties = !expandoColumn.getTypeSettingsProperties().equals(unicodeProperties);

                    if (changedType || changedProperties) {
                        expandoColumn.setType(dataType);
                        expandoColumn.setTypeSettingsProperties(unicodeProperties);
                        expandoColumnLocalService.updateExpandoColumn(expandoColumn);
                        _log.info(String.format("Updated custom field '%s' on model '%s'.", attributeName, modelResource));
                    }

                }
            }

            ExpandoColumn expandoColumn = getExpandoColumn(companyId, modelResource, attributeName);
            if (expandoColumn != null) {
                processedExpandoColumnIds.add(expandoColumn.getColumnId());
            }
        }

        // Delete missing custom fields (for 'DELETE' strategy)

        if (InitializerStrategy.DELETE.equals(strategy)) {
            // Get All Company Custom Fields
            DynamicQuery dynamicQuery = expandoColumnLocalService.dynamicQuery();
            dynamicQuery.add(RestrictionsFactoryUtil.eq("companyId", companyId));
            // Delete custom field (if not defined in initializer and not preserved)
            List<ExpandoColumn> expandoColumns = expandoColumnLocalService.dynamicQuery(dynamicQuery);
            for (ExpandoColumn expandoColumn : expandoColumns) {
                long columnId = expandoColumn.getColumnId();
                String expandoName = expandoColumn.getName();
                if (!processedExpandoColumnIds.contains(columnId) && !ArrayUtil.contains(PRESERVED_CUSTOM_FIELDS, expandoName)) {
                    expandoColumnLocalService.deleteColumn(expandoColumn);
                    _log.info(String.format("Deleted custom field #%d '%s' on model '%s'.", columnId, expandoName, expandoColumn.getModelClassName()));
                }
            }
        }

    }

    private ExpandoColumn getExpandoColumn(long companyId, String modelResource, String attributeName) {
        long classNameId = portal.getClassNameId(modelResource);
        ExpandoTable expandoTable = expandoTableLocalService.fetchDefaultTable(companyId, classNameId);

        long tableId = expandoTable.getTableId();
        return expandoColumnLocalService.getColumn(tableId, attributeName);
    }

    private UnicodeProperties getUnicodeProperties(JSONObject jsonObject) {
        UnicodeProperties unicodeProperties = new UnicodeProperties(true);;
        if (jsonObject.has("properties")) {
            JSONObject propertiesJSONObject = jsonObject.getJSONObject("properties");
            Map<String, Object> map = propertiesJSONObject.toMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                unicodeProperties.setProperty(
                        TextFormatter.format(entry.getKey(), TextFormatter.K),
                        String.valueOf(entry.getValue()));
            }
        }
        return unicodeProperties;
    }

    @Reference
    private Portal portal;
    @Reference
    private ExpandoTableLocalService expandoTableLocalService;
    @Reference
    private ExpandoColumnLocalService expandoColumnLocalService;

    private static final Log _log = LogFactoryUtil.getLog(ExpandoInitializerServiceImpl.class);

}
