package com.liferay.portal.site.initializer.internal.service.expando;

import com.liferay.portal.site.initializer.extender.context.InitializerContext;

public interface ExpandoInitializerService {

    void initializeCustomFields(InitializerContext context) throws Exception;

}