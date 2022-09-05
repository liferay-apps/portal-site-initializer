package com.liferay.portal.site.initializer.extender.util;

import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ClassedModel;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;

import java.io.Serializable;

public class ExpandoHelper {

    private static final boolean SECURED = false;

    public static void setCustomField(ClassedModel model, String fieldName, int fieldType, Serializable fieldValue) {
        setCustomField(model, fieldName, fieldType, fieldValue, null, null);
    }

    public static void setCustomField(ClassedModel model, String fieldName, int fieldType, Serializable fieldValue, Serializable defaultValue, UnicodeProperties unicodeProperties) {
        try {
            ExpandoBridge expandoBridge = model.getExpandoBridge();
            if (!expandoBridge.hasAttribute(fieldName)) {
                expandoBridge.addAttribute(fieldName, fieldType, SECURED);
                if (defaultValue != null) {
                    expandoBridge.setAttributeDefault(fieldName, defaultValue);
                }
                if (unicodeProperties != null) {
                    expandoBridge.setAttributeProperties(fieldName, unicodeProperties, SECURED);
                }
            }
            expandoBridge.setAttribute(fieldName, fieldValue, SECURED);
        } catch (Exception e) {
            _log.error("Can not set custom field, cause: " + e.getMessage());
        }
    }

    public static Serializable getCustomFiled(ClassedModel model, String filedName, Serializable defaultValue) {
        try {
            ExpandoBridge expandoBridge = model.getExpandoBridge();
            return expandoBridge.getAttribute(filedName, SECURED);
        } catch (Exception e) {
            _log.warn("Can not get custom field, return default one: " + defaultValue);
            return defaultValue;
        }
    }

    public static String getCustomFiled(ClassedModel model, String filedName) {
        Serializable customFiled = getCustomFiled(model, filedName, null);
        if (customFiled == null) {
            return null;
        }
        if (customFiled instanceof String) {
            return customFiled.toString();
        } else if (customFiled instanceof String[]) {
            String[] customFiledArray = (String[]) customFiled;
            return ArrayUtil.isNotEmpty(customFiledArray) ? customFiledArray[0] : null;
        }
        return null;
    }

    private static final Log _log = LogFactoryUtil.getLog(ExpandoHelper.class);

}