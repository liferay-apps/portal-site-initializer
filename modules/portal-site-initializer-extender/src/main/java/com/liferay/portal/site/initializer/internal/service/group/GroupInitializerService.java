package com.liferay.portal.site.initializer.internal.service.group;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.site.initializer.extender.model.InitializerStrategy;

import java.util.List;

public interface GroupInitializerService {

    void saveSiteInitializerKey(long groupId, String siteInitializerKey);

    void saveSiteInitializerStrategy(long groupId, InitializerStrategy strategy);

    InitializerStrategy getStrategy(long groupId);

    List<Group> getSiteInitializerSites(String siteInitializerKey);

}