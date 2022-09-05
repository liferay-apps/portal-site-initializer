package com.liferay.portal.site.initializer.extender.context;

import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.site.initializer.extender.model.InitializerStrategy;
import org.osgi.framework.Bundle;

import javax.servlet.ServletContext;

public class InitializerContext {

    private final ServiceContext serviceContext;
    private final ServletContext servletContext;
    private final Bundle bundle;
    private final InitializerStrategy strategy;

    public InitializerContext(ServiceContext serviceContext, ServletContext servletContext, Bundle bundle, InitializerStrategy strategy) {
        this.serviceContext = serviceContext;
        this.servletContext = servletContext;
        this.bundle = bundle;
        this.strategy = strategy;
    }

    public ServiceContext getServiceContext() {
        return serviceContext;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public InitializerStrategy getStrategy() {
        return strategy;
    }

}
