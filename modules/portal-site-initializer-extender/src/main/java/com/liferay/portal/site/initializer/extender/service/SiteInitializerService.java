package com.liferay.portal.site.initializer.extender.service;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.site.initializer.extender.model.InitializerStrategy;
import org.osgi.framework.Bundle;

import javax.servlet.ServletContext;
import java.util.List;

public interface SiteInitializerService {

    void initializeContent(long groupId, ServletContext servletContext, Bundle bundle, InitializerStrategy strategy) throws Exception;

    void saveSiteInitializerSettings(long groupId, String siteInitializerKey, InitializerStrategy strategy);

    List<Group> getSiteInitializerSites(String siteInitializerKey);

    InitializerStrategy getStrategy(long groupId);

}