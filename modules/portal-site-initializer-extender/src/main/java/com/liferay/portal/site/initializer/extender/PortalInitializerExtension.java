package com.liferay.portal.site.initializer.extender;

import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.site.initializer.extender.service.SiteInitializerService;
import com.liferay.site.initializer.SiteInitializer;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.osgi.framework.Bundle;

import javax.servlet.ServletContext;

public class PortalInitializerExtension {

    public PortalInitializerExtension(Bundle bundle, ServletContext servletContext, SiteInitializerService siteInitializerService) {

        _dependencyManager = new DependencyManager(bundle.getBundleContext());
        _component = _dependencyManager.createComponent();

        _portalSiteInitializer = new PortalSiteInitializer(bundle, siteInitializerService);
        _component.setImplementation(_portalSiteInitializer);

        _component.setInterface(SiteInitializer.class, MapUtil.singletonDictionary("site.initializer.key", bundle.getSymbolicName()));

        if (servletContext == null) {
            ServiceDependency serviceDependency = _dependencyManager.createServiceDependency();
            serviceDependency.setCallbacks("setServletContext", null);
            serviceDependency.setRequired(true);
            serviceDependency.setService(ServletContext.class,"(osgi.web.symbolicname=" + bundle.getSymbolicName() + ")");
            _component.add(serviceDependency);
        } else {
            _portalSiteInitializer.setServletContext(servletContext);
        }

    }

    public PortalSiteInitializer getPortalSiteInitializer() {
        return _portalSiteInitializer;
    }

    public ServletContext getServletContext() {
        return _portalSiteInitializer != null ? _portalSiteInitializer.getServletContext() : null;
    }

    public void destroy() {
        _dependencyManager.remove(_component);
    }

    public void start() {
        _dependencyManager.add(_component);
    }

    private final Component _component;
    private final DependencyManager _dependencyManager;
    private final PortalSiteInitializer _portalSiteInitializer;

}
