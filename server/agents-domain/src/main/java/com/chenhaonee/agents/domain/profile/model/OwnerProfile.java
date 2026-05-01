package com.chenhaonee.agents.domain.profile.model;

import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 单实例部署下的实例拥有者资料。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "owner_profile")
@EqualsAndHashCode(callSuper = true)
public class OwnerProfile extends BaseEntity {

    private String displayName;

    private String email;

    private String timezone;

    private String locale;

    private String barkDeviceKey;

    private String bio;

    public void updateProfile(String displayName, String email, String barkDeviceKey, String bio) {
        this.displayName = displayName;
        this.email = email;
        this.barkDeviceKey = barkDeviceKey;
        this.bio = bio;
    }

    public void updatePreferences(String timezone, String locale) {
        this.timezone = timezone;
        this.locale = locale;
    }

    public void rename(String displayName) {
        this.displayName = displayName;
    }
}
