package com.liferay.portal.site.initializer.internal.service.user.impl;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.site.initializer.extender.context.InitializerContext;
import com.liferay.portal.site.initializer.extender.model.InitializerStrategy;
import com.liferay.portal.site.initializer.internal.model.user.UsersConfig;
import com.liferay.portal.site.initializer.internal.model.user.UsersDto;
import com.liferay.portal.site.initializer.internal.service.user.UserInitializerService;
import com.liferay.portal.site.initializer.internal.util.SiteInitializerUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

@Component(service = UserInitializerService.class)
public class UserInitializerServiceImpl implements UserInitializerService {

    public static final String DESCRIPTOR_PATH = "/site-initializer/users.json";

    @Override
    public void initializeUsers(InitializerContext context) throws Exception {

        ServletContext servletContext = context.getServletContext();
        String json = SiteInitializerUtil.read(DESCRIPTOR_PATH, servletContext);
        if (Validator.isNull(json)) {
            return;
        }

        ServiceContext serviceContext = context.getServiceContext();
        long companyId = serviceContext.getCompanyId();
        long groupId = serviceContext.getScopeGroupId();

        Group currentGroup = groupLocalService.fetchGroup(groupId);
        String friendlyURL = currentGroup.getFriendlyURL();
        json = StringUtil.replace(json, "[$CURRENT_SITE$]", friendlyURL);

        InitializerStrategy strategy = context.getStrategy();

        UsersConfig usersConfig = JSONFactoryUtil.looseDeserialize(json, UsersConfig.class);
        List<String> defaultSites = usersConfig.getDefaultSites();
        List<Long> groupIdsArray = new ArrayList<>();
        if (ListUtil.isNotEmpty(defaultSites)) {
            for (String defaultSite: defaultSites) {
                Group group = groupLocalService.fetchFriendlyURLGroup(companyId, defaultSite);
                if (group != null) {
                    groupIdsArray.add(group.getGroupId());
                }
            }
        }
        long[] groupIds = ArrayUtil.toLongArray(groupIdsArray);

        long creatorUserId = 0;
        boolean autoScreenName = false;
        Locale locale = Locale.getDefault();
        String middleName = StringPool.BLANK;
        long prefixId = 0;
        long suffixId = 0;
        boolean male = true;
        int birthdayMonth = Calendar.JANUARY;
        int birthdayDay = 1;
        int birthdayYear = 1970;
        String jobTitle = StringPool.BLANK;
        long[] organizationIds = null;
        long[] userGroupIds = null;
        boolean sendEmail = false;

        List<Long> processedUserIds = new ArrayList<>();

        List<UsersDto> users = usersConfig.getUsers();
        for (UsersDto usersDto : users) {

            // User
            String emailAddress = usersDto.getEmailAddress();
            User user = userLocalService.fetchUserByEmailAddress(companyId, emailAddress);

            // User Roles
            List<String> roleNames = usersDto.getRoles();
            List<Long> roleIdsArray = new ArrayList<>();
            for (String roleName : roleNames) {
                Role role = roleLocalService.fetchRole(companyId, roleName);
                if (role != null) {
                    roleIdsArray.add(role.getRoleId());
                }
            }
            long[] roleIds = ArrayUtil.toLongArray(roleIdsArray);

            String password = usersDto.getPassword();
            boolean autoPassword = Validator.isNull(password);

            String screenName = usersDto.getScreenName();
            String firstName = usersDto.getFirstName();
            String lastName = usersDto.getLastName();

            if (user == null) {

                // Create User
                user = userLocalService.addUser(
                        creatorUserId,
                        companyId,
                        autoPassword,
                        password,
                        password,
                        autoScreenName,
                        screenName,
                        emailAddress,
                        locale,
                        firstName,
                        middleName,
                        lastName,
                        prefixId,
                        suffixId,
                        male,
                        birthdayMonth,
                        birthdayDay,
                        birthdayYear,
                        jobTitle,
                        groupIds,
                        organizationIds,
                        roleIds,
                        userGroupIds,
                        sendEmail,
                        serviceContext
                );
                _log.info(String.format("--- Created user #%d '%s'.", user.getUserId(), emailAddress));

            } else {

                if (!InitializerStrategy.CREATE.equals(strategy)) {

                    // Update User - if update is required, and if not 'CREATE' strategy
                    user = userLocalService.updateUser(
                            user.getUserId(),
                            null,
                            null,
                            null,
                            false,
                            user.getReminderQueryQuestion(),
                            user.getReminderQueryAnswer(),
                            screenName,
                            emailAddress,
                            false,
                            null,
                            user.getLanguageId(),
                            user.getTimeZoneId(),
                            user.getGreeting(),
                            user.getComments(),
                            firstName,
                            middleName,
                            lastName,
                            prefixId,
                            suffixId,
                            male,
                            birthdayMonth,
                            birthdayDay,
                            birthdayYear,
                            StringPool.BLANK,
                            StringPool.BLANK,
                            StringPool.BLANK,
                            StringPool.BLANK,
                            StringPool.BLANK,
                            jobTitle,
                            groupIds,
                            organizationIds,
                            roleIds,
                            null,
                            userGroupIds,
                            serviceContext
                    );
                    _log.info(String.format("--- Updated user #%d '%s'.", user.getUserId(), emailAddress));
                }

            }
            long userId = user.getUserId();
            processedUserIds.add(userId);
        }

        // Delete missing users (for 'DELETE' strategy)
        if (InitializerStrategy.DELETE.equals(strategy)) {
            List<User> portalUsers = userLocalService.getUsers(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
            for (User user: portalUsers) {
                long userId = user.getUserId();
                if (!processedUserIds.contains(userId)) {
                    _log.info(String.format("Deleted user #%d '%s'.", userId, user.getEmailAddress()));
                }
            }
        }

    }

    @Override
    public User getCurrentUser(long groupId) {
        // Get User from ThreadLocal
        long userId = PrincipalThreadLocal.getUserId();
        User user = userLocalService.fetchUser(userId);
        if (user == null) {
            // Get Creator User
            Group group = groupLocalService.fetchGroup(groupId);
            long creatorUserId = group.getCreatorUserId();
            user = userLocalService.fetchUser(creatorUserId);
        }
        return user;
    }

    @Reference
    private UserLocalService userLocalService;
    @Reference
    private RoleLocalService roleLocalService;
    @Reference
    private GroupLocalService groupLocalService;

    private static final Log _log = LogFactoryUtil.getLog(UserInitializerServiceImpl.class);

}
