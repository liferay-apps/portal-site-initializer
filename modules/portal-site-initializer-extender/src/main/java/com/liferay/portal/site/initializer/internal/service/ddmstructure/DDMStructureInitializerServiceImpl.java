package com.liferay.portal.site.initializer.internal.service.ddmstructure;

import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.site.initializer.extender.context.InitializerContext;
import com.liferay.portal.site.initializer.internal.service.ddmstructure.helper.DDMStructureHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Component(service = DDMStructureInitializerService.class)
public class DDMStructureInitializerServiceImpl implements DDMStructureInitializerService {

    @Override
    public void initializeDDMStructures(InitializerContext context) throws Exception {

        Map<String, String> ddmStructuresIdsStringUtilReplaceValues = new HashMap<>();

        ServletContext servletContext = context.getServletContext();
        Set<String> resourcePaths = servletContext.getResourcePaths("/site-initializer/ddm-structures");
        if (SetUtil.isEmpty(resourcePaths)) {
            return;
        }

        long classNameId = portal.getClassNameId(JournalArticle.class);
        for (String resourcePath : resourcePaths) {
            ddmStructureHelper.addDDMStructures(classNameId, resourcePath, context);
        }
    }

    @Reference
    private Portal portal;
    @Reference
    private DDMStructureHelper ddmStructureHelper;

}