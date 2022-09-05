package com.liferay.portal.site.initializer.extender;

import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.site.initializer.extender.model.InitializerStrategy;
import com.liferay.portal.site.initializer.extender.service.SiteInitializerService;
import com.liferay.portal.site.initializer.internal.backed.osgi.FileBackedBundleDelegate;
import com.liferay.portal.site.initializer.internal.backed.servlet.FileBackedServletContextDelegate;
import com.liferay.portal.util.PropsValues;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = PortalInitializerExtender.class)
public class PortalInitializerExtender implements BundleTrackerCustomizer<PortalInitializerExtension> {

    @Override
    public PortalInitializerExtension addingBundle(Bundle bundle, BundleEvent bundleEvent) {

        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);

        List<BundleCapability> bundleCapabilities = bundleWiring.getCapabilities("portal.site.initializer");
        if (ListUtil.isEmpty(bundleCapabilities)) {
            return null;
        }

        PortalInitializerExtension portalInitializerExtension = new PortalInitializerExtension(bundle, null, siteInitializerService);
        portalInitializerExtension.start();

        // Update Content for Existing Sites
        String siteInitializerKey = bundle.getSymbolicName();
        ServletContext servletContext = portalInitializerExtension.getServletContext();
        List<Group> siteInitializerSites = siteInitializerService.getSiteInitializerSites(siteInitializerKey);
        for (Group group : siteInitializerSites) {
            try {
                InitializerStrategy strategy = siteInitializerService.getStrategy(group.getGroupId());
                siteInitializerService.initializeContent(group.getGroupId(), servletContext, bundle, strategy);
            } catch (Exception e) {
                _log.error(String.format("Failed to initialize content for Site #%d '%s', cause: %s",
                        group.getGroupId(), group.getFriendlyURL(), e.getMessage()));
            }
        }

        return portalInitializerExtension;
    }

    public File getFile(String fileKey) {
        return _files.get(fileKey);
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent bundleEvent, PortalInitializerExtension portalInitializerExtension) {
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent bundleEvent, PortalInitializerExtension portalInitializerExtension) {
        portalInitializerExtension.destroy();
    }

    @Activate
    protected void activate(BundleContext bundleContext) throws Exception {
        _bundleContext = bundleContext;
        _bundleTracker = new BundleTracker<>(bundleContext, Bundle.ACTIVE, this);
        _bundleTracker.open();
        File siteInitializersDirectoryFile = new File(PropsValues.LIFERAY_HOME, "site-initializers");
        if (siteInitializersDirectoryFile.isDirectory()) {
            for (File file : siteInitializersDirectoryFile.listFiles()) {
                _addFile(file);
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        _bundleTracker.close();
        _files.clear();
        for (PortalInitializerExtension siteInitializerExtension: _fileSiteInitializerExtensions) {
            siteInitializerExtension.destroy();
        }
        _fileSiteInitializerExtensions.clear();
    }

    private void _addFile(File file) throws Exception {
        if (!file.isDirectory()) {
            return;
        }
        String fileKey = StringUtil.randomString(16);
        _files.put(fileKey, file);
        String symbolicName = "Liferay Site Initializer - File - " + fileKey;
        Bundle bundle = ProxyUtil.newDelegateProxyInstance(
                Bundle.class.getClassLoader(),
                Bundle.class,
                new FileBackedBundleDelegate(_bundleContext, file, jsonFactory, symbolicName),
                null
        );
        ServletContext servletContext = ProxyUtil.newDelegateProxyInstance(
                ServletContext.class.getClassLoader(), ServletContext.class,
                new FileBackedServletContextDelegate(file, fileKey, symbolicName),
                null
        );
        PortalInitializerExtension portalInitializerExtension = new PortalInitializerExtension(
                bundle,
                servletContext,
                siteInitializerService
        );
        portalInitializerExtension.start();
        _fileSiteInitializerExtensions.add(portalInitializerExtension);
    }

    @Reference
    private JSONFactory jsonFactory;
    @Reference
    private SiteInitializerService siteInitializerService;

    private BundleContext _bundleContext;
    private BundleTracker<?> _bundleTracker;

    private final Map<String, File> _files = new HashMap<>();
    private final List<PortalInitializerExtension> _fileSiteInitializerExtensions = new ArrayList<>();

    private static final Log _log = LogFactoryUtil.getLog(PortalInitializerExtender.class);

}