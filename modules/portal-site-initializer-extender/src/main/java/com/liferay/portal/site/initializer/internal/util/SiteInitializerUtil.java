package com.liferay.portal.site.initializer.internal.util;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.*;
import com.liferay.portal.vulcan.util.ObjectMapperUtil;
import org.osgi.framework.Bundle;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiteInitializerUtil {

    public static String read(Bundle bundle, String fileName, URL url) throws Exception {
        String urlPath = url.getPath();
        URL entryURL = bundle.getEntry(urlPath.substring(0, urlPath.lastIndexOf("/") + 1) + fileName);
        try (InputStream inputStream = entryURL.openStream()) {
            return StringUtil.read(entryURL.openStream());
        }
    }

    public static String read(String resourcePath, ServletContext servletContext) throws Exception {
        try (InputStream inputStream = servletContext.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return null;
            }
            String content = StringUtil.read(inputStream);
            Map<String, String> portalPropertiesStringUtilReplaceValues = _getPortalPropertiesStringUtilReplaceValues(content);
            return StringUtil.replace(content, "[$", "$]", portalPropertiesStringUtilReplaceValues);
        }
    }

    public static Map<Locale, String> toMap(String values) {
        return toMap(StringPool.BLANK, values);
    }

    public static Map<Locale, String> toMap(String prefix, String values) {
        if (Validator.isBlank(values)) {
            return Collections.emptyMap();
        }
        Map<Locale, String> map = new HashMap<>();
        Map<String, String> valuesMap = ObjectMapperUtil.readValue(HashMap.class, values);
        for (Map.Entry<String, String> entry : valuesMap.entrySet()) {
            map.put(LocaleUtil.fromLanguageId(entry.getKey()), prefix + entry.getValue());
        }
        return map;
    }

    private static Map<String, String> _getPortalPropertiesStringUtilReplaceValues(String content) {

        Map<String, String> portalPropertiesStringUtilReplaceValues = new HashMap<>();
        if (Validator.isNull(content)) {
            return portalPropertiesStringUtilReplaceValues;
        }

        Matcher matcher = _portalPropertyPattern.matcher(content);

        while (matcher.find()) {
            String portalProperty = matcher.group();
            portalProperty = portalProperty.substring(2, portalProperty.length() - 2);
            String[] portalPropertyParts = StringUtil.split(portalProperty, CharPool.COLON);
            String value = PropsUtil.get(portalPropertyParts[1]);
            if ((value == null) || !ArrayUtil.contains(_PORTAL_PROPERTIES_KEYS_WHITELIST, portalPropertyParts[1])) {
                value = StringPool.BLANK;
            }
            portalPropertiesStringUtilReplaceValues.put(portalProperty, value);
        }

        return portalPropertiesStringUtilReplaceValues;
    }

    private static final String[] _PORTAL_PROPERTIES_KEYS_WHITELIST = {
            "default.guest.public.layout.friendly.url",
            "default.guest.public.layout.name",
            "default.guest.public.layout.regular.color.scheme.id",
            "default.guest.public.layout.regular.theme.id",
            "default.guest.public.layout.template.id"
    };

    private static final Pattern _portalPropertyPattern = Pattern.compile("\\[\\$PORTAL_PROPERTY:((?!\\.)(?!.*\\.\\.)[a-zA-Z0-9_.]+)\\$\\]");


}
