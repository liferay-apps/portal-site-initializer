package com.liferay.portal.site.initializer.internal.service.group.impl;

import com.liferay.expando.kernel.model.ExpandoColumnConstants;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.site.initializer.extender.model.InitializerStrategy;
import com.liferay.portal.site.initializer.internal.service.group.GroupInitializerService;
import com.liferay.portal.site.initializer.extender.util.ExpandoHelper;
import com.liferay.portal.site.initializer.extender.util.ExpandoKeys;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.ArrayList;
import java.util.List;

@Component(service = GroupInitializerService.class)
public class GroupInitializerServiceImpl implements GroupInitializerService {

    @Override
    public void saveSiteInitializerKey(long groupId, String siteInitializerKey) {
        Group group = groupLocalService.fetchGroup(groupId);
        ExpandoHelper.setCustomField(group, ExpandoKeys.SI_KEY, ExpandoColumnConstants.STRING, siteInitializerKey);
    }

    @Override
    public void saveSiteInitializerStrategy(long groupId, InitializerStrategy strategy) {
        Group group = groupLocalService.fetchGroup(groupId);
        String expandoValue = strategy.name();
        String[] defaultValues = new String[] {
            InitializerStrategy.CREATE.name(),
            InitializerStrategy.UPDATE.name(),
            InitializerStrategy.DELETE.name()
        };
        UnicodeProperties unicodeProperties = new UnicodeProperties(true);
        unicodeProperties.setProperty(ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE, ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_SELECTION_LIST);
        ExpandoHelper.setCustomField(group, ExpandoKeys.SI_STRATEGY, ExpandoColumnConstants.STRING_ARRAY, expandoValue, defaultValues, unicodeProperties);
    }

    @Override
    public InitializerStrategy getStrategy(long groupId) {
        Group group = groupLocalService.fetchGroup(groupId);
        String strategy = ExpandoHelper.getCustomFiled(group, ExpandoKeys.SI_STRATEGY);
        return InitializerStrategy.fromName(strategy);
    }

    @Override
    public List<Group> getSiteInitializerSites(String siteInitializerKey) {
        List<Group> siteInitializerSites = new ArrayList<>();
        List<Group> groups = groupLocalService.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
        for (Group group: groups) {
            String siKey = ExpandoHelper.getCustomFiled(group, ExpandoKeys.SI_KEY);
            if (StringUtil.equals(siKey, siteInitializerKey)) {
                siteInitializerSites.add(group);
            }
        }
        return siteInitializerSites;
    }

    @Reference
    private GroupLocalService groupLocalService;

}
