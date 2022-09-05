package com.liferay.portal.site.initializer.internal.service.user;

import com.liferay.portal.kernel.model.User;
import com.liferay.portal.site.initializer.extender.context.InitializerContext;

public interface UserInitializerService {

    void initializeUsers(InitializerContext context) throws Exception;

    User getCurrentUser(long groupId);

}