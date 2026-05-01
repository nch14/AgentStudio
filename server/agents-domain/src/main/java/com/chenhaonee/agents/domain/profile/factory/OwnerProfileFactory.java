package com.chenhaonee.agents.domain.profile.factory;

import com.chenhaonee.agents.common.validator.ValidationUtils;
import com.chenhaonee.agents.domain.profile.model.OwnerProfile;
import org.springframework.stereotype.Component;

/**
 * Owner Profile 工厂。
 */
@Component
public class OwnerProfileFactory {

    public OwnerProfile create(String displayName, String timezone, String locale) {
        return create(displayName, null, timezone, locale, null, null);
    }

    public OwnerProfile create(String displayName, String email, String timezone, String locale,
                               String barkDeviceKey, String bio) {
        OwnerProfile profile = new OwnerProfile();
        profile.setDisplayName(ValidationUtils.requireText(displayName, "displayName"));
        profile.setEmail(email);
        profile.setTimezone(ValidationUtils.requireText(timezone, "timezone"));
        profile.setLocale(ValidationUtils.requireText(locale, "locale"));
        profile.setBarkDeviceKey(barkDeviceKey);
        profile.setBio(bio);
        return profile;
    }
}
