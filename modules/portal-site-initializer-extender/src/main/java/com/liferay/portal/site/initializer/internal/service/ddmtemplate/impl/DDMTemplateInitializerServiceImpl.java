package com.liferay.portal.site.initializer.internal.service.ddmtemplate.impl;

import com.liferay.dynamic.data.mapping.constants.DDMTemplateConstants;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.template.TemplateConstants;
import com.liferay.portal.kernel.util.*;
import com.liferay.portal.site.initializer.extender.context.InitializerContext;
import com.liferay.portal.site.initializer.internal.service.ddmtemplate.DDMTemplateInitializerService;
import com.liferay.portal.site.initializer.internal.util.SiteInitializerUtil;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.net.URL;
import java.util.Enumeration;

@Component(service = DDMTemplateInitializerService.class)
public class DDMTemplateInitializerServiceImpl implements DDMTemplateInitializerService {

    @Override
    public void initializeDDMTemplates(InitializerContext context) throws Exception {

        Bundle bundle = context.getBundle();
        ServiceContext serviceContext = context.getServiceContext();

        Enumeration<URL> enumeration = bundle.findEntries("/site-initializer/ddm-templates", "ddm-template.json", true);
        if (enumeration == null) {
            return;
        }

        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            JSONObject jsonObject = JSONFactoryUtil.createJSONObject(StringUtil.read(url.openStream()));
            long resourceClassNameId = _portal.getClassNameId(
                    jsonObject.getString("resourceClassName", JournalArticle.class.getName()));

            long ddmStructureId = 0;
            String ddmStructureKey = jsonObject.getString("ddmStructureKey");

            if (Validator.isNotNull(ddmStructureKey)) {
                DDMStructure ddmStructure =
                        _ddmStructureLocalService.fetchStructure(
                                serviceContext.getScopeGroupId(), resourceClassNameId,
                                ddmStructureKey);

                ddmStructureId = ddmStructure.getStructureId();
            }

            DDMTemplate ddmTemplate = _ddmTemplateLocalService.fetchTemplate(
                    serviceContext.getScopeGroupId(),
                    _portal.getClassNameId(
                            jsonObject.getString(
                                    "className", DDMStructure.class.getName())),
                    jsonObject.getString("ddmTemplateKey"));
            if (ddmTemplate == null) {
                _ddmTemplateLocalService.addTemplate(
                        serviceContext.getUserId(),
                        serviceContext.getScopeGroupId(),
                        _portal.getClassNameId(
                                jsonObject.getString(
                                        "className", DDMStructure.class.getName())),
                        ddmStructureId, resourceClassNameId,
                        jsonObject.getString("ddmTemplateKey"),
                        HashMapBuilder.put(
                                LocaleUtil.getSiteDefault(),
                                jsonObject.getString("name")
                        ).build(),
                        null, DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY, null,
                        TemplateConstants.LANG_TYPE_FTL,
                        SiteInitializerUtil.read(bundle, "ddm-template.ftl", url),
                        false, false, null, null, serviceContext);
            }
            else {
                _ddmTemplateLocalService.updateTemplate(
                        serviceContext.getUserId(), ddmTemplate.getTemplateId(),
                        ddmStructureId,
                        HashMapBuilder.put(
                                LocaleUtil.getSiteDefault(),
                                jsonObject.getString("name")
                        ).build(),
                        null, DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY, null,
                        TemplateConstants.LANG_TYPE_FTL,
                        SiteInitializerUtil.read(bundle, "ddm-template.ftl", url),
                        false, false, null, null, serviceContext);
            }

        }
    }

    @Reference
    private Portal _portal;
    @Reference
    private DDMStructureLocalService _ddmStructureLocalService;
    @Reference
    private DDMTemplateLocalService _ddmTemplateLocalService;

}