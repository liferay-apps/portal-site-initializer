package com.liferay.portal.site.initializer.internal.service.ddmstructure.helper;

import com.liferay.portal.site.initializer.extender.context.InitializerContext;

public interface DDMStructureHelper {

    void addDDMStructures(long classNameId, String fileName, InitializerContext initializerContext) throws Exception;

}