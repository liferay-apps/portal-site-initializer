package com.liferay.portal.site.initializer.internal.service;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.site.initializer.extender.context.InitializerContext;
import com.liferay.portal.site.initializer.extender.model.InitializerStrategy;
import com.liferay.portal.site.initializer.internal.service.ddmstructure.DDMStructureInitializerService;
import com.liferay.portal.site.initializer.internal.service.ddmtemplate.DDMTemplateInitializerService;
import com.liferay.portal.site.initializer.internal.service.expando.ExpandoInitializerService;
import com.liferay.portal.site.initializer.internal.service.group.GroupInitializerService;
import com.liferay.portal.site.initializer.internal.service.role.RoleInitializerService;
import com.liferay.portal.site.initializer.internal.service.user.UserInitializerService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;

@Component(service = SiteInitializerHelper.class)
public class SiteInitializerHelperImpl implements SiteInitializerHelper {

    @Override
    public void initializeCustomFields(InitializerContext context) throws Exception {
        expandoInitializerService.initializeCustomFields(context);
    }

    @Override
    public void initializeRoles(InitializerContext context) throws Exception {
        roleInitializerService.initializeRoles(context);
    }

    @Override
    public void initializeUsers(InitializerContext context) throws Exception {
        userInitializerService.initializeUsers(context);
    }

    @Override
    public void initializeDDMStructures(InitializerContext context) throws Exception {
        ddmStructureInitializerService.initializeDDMStructures(context);
    }

    @Override
    public void initializeDDMTemplates(InitializerContext context) throws Exception {
        ddmTemplateInitializerService.initializeDDMTemplates(context);
    }

    @Override
    public User getCurrentUser(long groupId) {
        return userInitializerService.getCurrentUser(groupId);
    }

    @Override
    public void saveSiteInitializerSettings(long groupId, String siteInitializerKey, InitializerStrategy strategy) {
        groupInitializerService.saveSiteInitializerKey(groupId, siteInitializerKey);
        groupInitializerService.saveSiteInitializerStrategy(groupId, strategy);
    }

    @Override
    public List<Group> getSiteInitializerSites(String siteInitializerKey) {
        return groupInitializerService.getSiteInitializerSites(siteInitializerKey);
    }

    @Override
    public InitializerStrategy getStrategy(long groupId) {
        return groupInitializerService.getStrategy(groupId);
    }

    @Reference
    private UserInitializerService userInitializerService;
    @Reference
    private RoleInitializerService roleInitializerService;
    @Reference
    private GroupInitializerService groupInitializerService;
    @Reference
    private ExpandoInitializerService expandoInitializerService;
    @Reference
    private DDMTemplateInitializerService ddmTemplateInitializerService;
    @Reference
    private DDMStructureInitializerService ddmStructureInitializerService;

}