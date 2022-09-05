package com.liferay.portal.site.initializer.internal.backed.servlet;

import com.liferay.portal.site.initializer.internal.backed.util.PathUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FileBackedServletContextDelegate {


    public FileBackedServletContextDelegate(
            File file, String fileKey, String servletContextName) {

        _file = file;
        _fileKey = fileKey;
        _servletContextName = servletContextName;
    }

    public String getContextPath() {
        return "/o/file-backed-portal-initializer/" + _fileKey;
    }

    public URL getResource(String path) throws MalformedURLException {
        File file = new File(_file, PathUtil.removePrefix(path));

        if (file.exists()) {
            URI uri = file.toURI();

            return uri.toURL();
        }

        return null;
    }

    public InputStream getResourceAsStream(String path) {
        File file = new File(_file, PathUtil.removePrefix(path));

        if (file.exists()) {
            try {
                return new FileInputStream(file);
            }
            catch (FileNotFoundException fileNotFoundException) {
                throw new IllegalStateException(fileNotFoundException);
            }
        }

        return null;
    }

    public Set<String> getResourcePaths(String path) {
        File searchDirectoryFile = new File(_file, PathUtil.removePrefix(path));

        if (!searchDirectoryFile.exists()) {
            return Collections.emptySet();
        }

        Set<String> resourcePaths = new HashSet<>();

        if (!path.endsWith("/")) {
            path = path.concat("/");
        }

        for (File file : searchDirectoryFile.listFiles()) {
            String resourcePath = path.concat(file.getName());

            if (file.isDirectory()) {
                resourcePath = resourcePath.concat("/");
            }

            resourcePaths.add(resourcePath);
        }

        return resourcePaths;
    }

    public String getServletContextName() {
        return _servletContextName;
    }

    private final File _file;
    private final String _fileKey;
    private final String _servletContextName;


}
