package com.liferay.portal.site.initializer.internal.service.role.impl;

import com.liferay.account.constants.AccountConstants;
import com.liferay.account.model.AccountRole;
import com.liferay.account.service.AccountRoleLocalService;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.site.initializer.extender.context.InitializerContext;
import com.liferay.portal.site.initializer.internal.service.role.RoleInitializerService;
import com.liferay.portal.site.initializer.internal.util.SiteInitializerUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.util.Collections;

@Component(service = RoleInitializerService.class)
public class RoleInitializerServiceServiceImpl implements RoleInitializerService {

    public static final String DESCRIPTOR_PATH = "/site-initializer/roles.json";

    @Override
    public void initializeRoles(InitializerContext context) throws Exception {

        ServletContext servletContext = context.getServletContext();
        String json = SiteInitializerUtil.read(DESCRIPTOR_PATH, servletContext);
        if (Validator.isNull(json)) {
            return;
        }

        ServiceContext serviceContext = context.getServiceContext();

        JSONArray jsonArray = jsonFactory.createJSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
            _addRole(jsonArray.getJSONObject(i), serviceContext);
        }

    }

    private void _addRole(JSONObject jsonObject, ServiceContext serviceContext) throws Exception {

        String name = jsonObject.getString("name");

        Role role = roleLocalService.fetchRole(serviceContext.getCompanyId(), name);

        if (role == null) {
            if (jsonObject.getInt("type") == RoleConstants.TYPE_ACCOUNT) {
                AccountRole accountRole = accountRoleLocalService.addAccountRole(
                        serviceContext.getUserId(),
                        AccountConstants.ACCOUNT_ENTRY_ID_DEFAULT, name,
                        Collections.singletonMap(serviceContext.getLocale(), name),
                        SiteInitializerUtil.toMap(jsonObject.getString("description")));
                role = accountRole.getRole();
            } else {
                role = roleLocalService.addRole(
                        serviceContext.getUserId(), null, 0, name,
                        Collections.singletonMap(serviceContext.getLocale(), name),
                        SiteInitializerUtil.toMap(jsonObject.getString("description")),
                        jsonObject.getInt("type"), jsonObject.getString("subtype"),
                        serviceContext);
            }
            _log.info(String.format("Created role #%d '%s'.", role.getRoleId(), role.getName()));
        }

        JSONArray jsonArray = jsonObject.getJSONArray("actions");

        if (JSONUtil.isEmpty(jsonArray)) {
            return;
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject actionsJSONObject = jsonArray.getJSONObject(i);

            String resource = actionsJSONObject.getString("resource");
            int scope = actionsJSONObject.getInt("scope");
            String actionId = actionsJSONObject.getString("actionId");

            if (scope == ResourceConstants.SCOPE_COMPANY) {
                resourcePermissionLocalService.addResourcePermission(
                        serviceContext.getCompanyId(), resource, scope,
                        String.valueOf(role.getCompanyId()), role.getRoleId(),
                        actionId);
            }
            else if (scope == ResourceConstants.SCOPE_GROUP) {
                resourcePermissionLocalService.removeResourcePermissions(
                        serviceContext.getCompanyId(), resource,
                        ResourceConstants.SCOPE_GROUP, role.getRoleId(), actionId);

                resourcePermissionLocalService.addResourcePermission(
                        serviceContext.getCompanyId(), resource,
                        ResourceConstants.SCOPE_GROUP,
                        String.valueOf(serviceContext.getScopeGroupId()),
                        role.getRoleId(), actionId);
            }
            else if (scope == ResourceConstants.SCOPE_GROUP_TEMPLATE) {
                resourcePermissionLocalService.addResourcePermission(
                        serviceContext.getCompanyId(), resource,
                        ResourceConstants.SCOPE_GROUP_TEMPLATE,
                        String.valueOf(GroupConstants.DEFAULT_PARENT_GROUP_ID),
                        role.getRoleId(), actionId);
            }
        }
    }


    @Reference
    private JSONFactory jsonFactory;
    @Reference
    private RoleLocalService roleLocalService;
    @Reference
    private AccountRoleLocalService accountRoleLocalService;
    @Reference
    private ResourcePermissionLocalService resourcePermissionLocalService;

    private static final Log _log = LogFactoryUtil.getLog(RoleInitializerServiceServiceImpl.class);

}
