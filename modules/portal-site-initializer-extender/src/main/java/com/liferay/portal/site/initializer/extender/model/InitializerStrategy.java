package com.liferay.portal.site.initializer.extender.model;

public enum InitializerStrategy {

    CREATE,     // Only create new content
    UPDATE,     // Create new and update existing content
    DELETE;     // Create new, update existing and remove missing content

    public static InitializerStrategy fromName(String name) {
        InitializerStrategy strategy = null;
        try {
            strategy = InitializerStrategy.valueOf(name);
        } catch (Exception ignored) {
        }
        if (strategy == null) {
            strategy = InitializerStrategy.UPDATE;
        }
        return strategy;
    }

}