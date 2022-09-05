package com.liferay.portal.site.initializer.extender;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.site.initializer.extender.model.InitializerStrategy;
import com.liferay.portal.site.initializer.extender.service.SiteInitializerService;
import com.liferay.site.exception.InitializationException;
import com.liferay.site.initializer.SiteInitializer;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

import javax.servlet.ServletContext;
import java.util.Dictionary;
import java.util.Locale;

public class PortalSiteInitializer implements SiteInitializer {

    public PortalSiteInitializer(Bundle bundle, SiteInitializerService siteInitializerService) {
        _bundle = bundle;
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        _classLoader = bundleWiring.getClassLoader();
        _siteInitializerService = siteInitializerService;
    }

    @Override
    public String getDescription(Locale locale) {
        Dictionary<String, String> headers = _bundle.getHeaders(StringPool.BLANK);
        return GetterUtil.getString(headers.get("Portal-Site-Initializer-Description"));
    }

    @Override
    public String getKey() {
        return _bundle.getSymbolicName();
    }

    @Override
    public String getName(Locale locale) {
        Dictionary<String, String> headers = _bundle.getHeaders(StringPool.BLANK);
        return GetterUtil.getString(headers.get("Portal-Site-Initializer-Name"), headers.get("Bundle-Name"));
    }

    public InitializerStrategy getStrategy() {
        Dictionary<String, String> headers = _bundle.getHeaders(StringPool.BLANK);
        String strategy = GetterUtil.getString(headers.get("Portal-Site-Initializer-Strategy"), InitializerStrategy.DELETE.name());
        return InitializerStrategy.fromName(strategy);
    }

    @Override
    public String getThumbnailSrc() {
        return _servletContext.getContextPath() + "/thumbnail.png";
    }

    @Override
    public void initialize(long groupId) throws InitializationException {
        long startTime = System.currentTimeMillis();
        if (_log.isInfoEnabled()) {
            _log.info(StringBundler.concat("Initializing ", getKey(), " for group ", groupId));
        }

        try {
            // Initialize Content
            _siteInitializerService.initializeContent(groupId, _servletContext, _bundle, getStrategy());

            // Save Site Initializer reference and Strategy to site
            _siteInitializerService.saveSiteInitializerSettings(groupId, getKey(), getStrategy());
        }
        catch (Exception exception) {
            _log.error(exception);
            throw new InitializationException(exception);
        }

        if (_log.isInfoEnabled()) {
            _log.info(StringBundler.concat("Initialized ", getKey(), " for group ", groupId, " in ", System.currentTimeMillis() - startTime, " ms"));
        }
    }

    @Override
    public boolean isActive(long companyId) {
        return true;
    }

    protected void setServletContext(ServletContext servletContext) {
        _servletContext = servletContext;
    }

    public ServletContext getServletContext() {
        return _servletContext;
    }

    private final Bundle _bundle;
    private final ClassLoader _classLoader;
    private final SiteInitializerService _siteInitializerService;
    private ServletContext _servletContext;

    private static final Log _log = LogFactoryUtil.getLog(PortalSiteInitializer.class);

}
