package com.liferay.portal.site.initializer.extender.service.impl;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.site.initializer.extender.context.InitializerContext;
import com.liferay.portal.site.initializer.extender.model.InitializerStrategy;
import com.liferay.portal.site.initializer.extender.service.SiteInitializerService;
import com.liferay.portal.site.initializer.internal.service.SiteInitializerHelper;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.util.List;

@Component(service = SiteInitializerService.class)
public class SiteInitializerServiceImpl implements SiteInitializerService {

    @Override
    public void initializeContent(long groupId, ServletContext servletContext, Bundle bundle, InitializerStrategy strategy) throws Exception {
        _log.info("START content initialization for Site #" + groupId);

        // Prepare Context
        InitializerContext context = getInitializerContext(groupId, servletContext, bundle, strategy);

        siteInitializerHelper.initializeCustomFields(context);
        siteInitializerHelper.initializeRoles(context);
        siteInitializerHelper.initializeUsers(context);

        siteInitializerHelper.initializeDDMStructures(context);
        siteInitializerHelper.initializeDDMTemplates(context);

        _log.info("END content initialization for Site #" + groupId);
    }

    @Override
    public void saveSiteInitializerSettings(long groupId, String siteInitializerKey, InitializerStrategy strategy) {
        siteInitializerHelper.saveSiteInitializerSettings(groupId, siteInitializerKey, strategy);
    }

    @Override
    public List<Group> getSiteInitializerSites(String siteInitializerKey) {
        return siteInitializerHelper.getSiteInitializerSites(siteInitializerKey);
    }

    @Override
    public InitializerStrategy getStrategy(long groupId) {
        return siteInitializerHelper.getStrategy(groupId);
    }

    private InitializerContext getInitializerContext(long groupId, ServletContext servletContext, Bundle bundle, InitializerStrategy strategy) {
        User user = siteInitializerHelper.getCurrentUser(groupId);
        ServiceContext serviceContext = createServiceContext();
        serviceContext.setAddGroupPermissions(true);
        serviceContext.setAddGuestPermissions(true);
        serviceContext.setCompanyId(user.getCompanyId());
        serviceContext.setScopeGroupId(groupId);
        serviceContext.setTimeZone(user.getTimeZone());
        serviceContext.setUserId(user.getUserId());
        return new InitializerContext(serviceContext, servletContext, bundle, strategy);
    }

    private ServiceContext createServiceContext() {
        ServiceContext serviceContextThreadLocal = ServiceContextThreadLocal.getServiceContext();
        return serviceContextThreadLocal != null
                ? (ServiceContext)serviceContextThreadLocal.clone()
                : new ServiceContext();
    }

    @Reference
    private SiteInitializerHelper siteInitializerHelper;

    private static final Log _log = LogFactoryUtil.getLog(SiteInitializerServiceImpl.class);

}
