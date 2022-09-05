package com.liferay.portal.site.initializer.internal.model.user;

import java.io.Serializable;
import java.util.List;

public class UsersConfig implements Serializable {

    private List<String> defaultSites;
    private List<UsersDto> users;

    public List<String> getDefaultSites() {
        return defaultSites;
    }

    public void setDefaultSites(List<String> defaultSites) {
        this.defaultSites = defaultSites;
    }

    public List<UsersDto> getUsers() {
        return users;
    }

    public void setUsers(List<UsersDto> users) {
        this.users = users;
    }
}
