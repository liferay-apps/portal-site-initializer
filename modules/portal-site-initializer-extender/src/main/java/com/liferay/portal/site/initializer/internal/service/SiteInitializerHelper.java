package com.liferay.portal.site.initializer.internal.service;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.site.initializer.extender.context.InitializerContext;
import com.liferay.portal.site.initializer.extender.model.InitializerStrategy;

import java.util.List;

public interface SiteInitializerHelper {

    void initializeCustomFields(InitializerContext context) throws Exception;

    void initializeRoles(InitializerContext context) throws Exception;

    void initializeUsers(InitializerContext context) throws Exception;

    void initializeDDMStructures(InitializerContext context) throws Exception;

    void initializeDDMTemplates(InitializerContext context) throws Exception;

    User getCurrentUser(long groupId);

    void saveSiteInitializerSettings(long groupId, String siteInitializerKey, InitializerStrategy strategy);

    List<Group> getSiteInitializerSites(String siteInitializerKey);

    InitializerStrategy getStrategy(long groupId);

}
