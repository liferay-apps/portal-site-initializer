package com.liferay.portal.site.initializer.internal.service.role;

import com.liferay.portal.site.initializer.extender.context.InitializerContext;

public interface RoleInitializerService {

    void initializeRoles(InitializerContext context) throws Exception;

}
