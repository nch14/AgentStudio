package com.chenhaonee.agents.domain.profile.service;

import com.chenhaonee.agents.domain.profile.factory.OwnerProfileFactory;
import com.chenhaonee.agents.domain.profile.model.OwnerProfile;
import com.chenhaonee.agents.domain.profile.repository.OwnerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 拥有者资料领域服务。
 */
@Service
public class OwnerProfileDomainService {

    private final OwnerProfileRepository ownerProfileRepository;
    private final OwnerProfileFactory ownerProfileFactory;

    public OwnerProfileDomainService(OwnerProfileRepository ownerProfileRepository,
                                     OwnerProfileFactory ownerProfileFactory) {
        this.ownerProfileRepository = ownerProfileRepository;
        this.ownerProfileFactory = ownerProfileFactory;
    }

    public OwnerProfile requireCurrent() {
        return ownerProfileRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("owner profile has not been initialized"));
    }

    @Transactional(rollbackFor = Exception.class)
    public OwnerProfile create(String displayName, String email, String barkDeviceKey, String bio,
                               String timezone, String locale) {
        if (ownerProfileRepository.count() > 0) {
            throw new IllegalStateException("owner profile already exists");
        }
        OwnerProfile profile = ownerProfileFactory.create(displayName, email, timezone, locale, barkDeviceKey, bio);
        return ownerProfileRepository.save(profile);
    }

    @Transactional(rollbackFor = Exception.class)
    public OwnerProfile updateProfile(String displayName, String email, String barkDeviceKey, String bio) {
        OwnerProfile profile = requireCurrent();
        profile.updateProfile(displayName, email, barkDeviceKey, bio);
        return ownerProfileRepository.save(profile);
    }

    @Transactional(rollbackFor = Exception.class)
    public OwnerProfile updatePreferences(String timezone, String locale) {
        OwnerProfile profile = requireCurrent();
        profile.updatePreferences(timezone, locale);
        return ownerProfileRepository.save(profile);
    }

    @Transactional(rollbackFor = Exception.class)
    public OwnerProfile rename(String displayName) {
        OwnerProfile profile = requireCurrent();
        profile.rename(displayName);
        return ownerProfileRepository.save(profile);
    }
}
