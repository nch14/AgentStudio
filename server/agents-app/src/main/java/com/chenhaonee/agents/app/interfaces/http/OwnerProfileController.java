package com.chenhaonee.agents.app.interfaces.http;

import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.domain.profile.model.OwnerProfile;
import com.chenhaonee.agents.domain.profile.service.OwnerProfileDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * 用户资料接口。
 */
@Tag(name = "Profile", description = "用户资料管理")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class OwnerProfileController {

    private static final Logger log = LoggerFactory.getLogger(OwnerProfileController.class);

    private final OwnerProfileDomainService ownerProfileDomainService;

    @Operation(summary = "创建用户资料（仅初始化时调用一次）")
    @PostMapping
    public Response<ProfileDto> create(@RequestBody CreateProfileRequest request) {
        try {
            OwnerProfile profile = ownerProfileDomainService.create(
                    request.displayName(),
                    request.email(),
                    request.barkDeviceKey(),
                    request.bio(),
                    request.timezone(),
                    request.locale());
            return Response.success(ProfileDto.from(profile));
        } catch (Exception e) {
            log.error("Failed to create owner profile", e);
            return Response.error(500, e.getMessage());
        }
    }

    @Operation(summary = "获取当前用户资料")
    @GetMapping
    public Response<ProfileDto> get() {
        try {
            OwnerProfile profile = ownerProfileDomainService.requireCurrent();
            return Response.success(ProfileDto.from(profile));
        } catch (IllegalStateException e) {
            // owner profile not initialized, return empty data gracefully
            return Response.success(null);
        } catch (Exception e) {
            log.error("Failed to get owner profile", e);
            return Response.error(500, e.getMessage());
        }
    }

    @Operation(summary = "更新用户资料")
    @PutMapping
    public Response<ProfileDto> update(@RequestBody UpdateProfileRequest request) {
        try {
            OwnerProfile profile = ownerProfileDomainService.updateProfile(
                    request.displayName(),
                    request.email(),
                    request.barkDeviceKey(),
                    request.bio());
            return Response.success(ProfileDto.from(profile));
        } catch (Exception e) {
            log.error("Failed to update owner profile", e);
            return Response.error(500, e.getMessage());
        }
    }

    public record ProfileDto(
            String displayName,
            String email,
            String timezone,
            String locale,
            String barkDeviceKey,
            String bio) {

        static ProfileDto from(OwnerProfile p) {
            return new ProfileDto(
                    p.getDisplayName(),
                    p.getEmail(),
                    p.getTimezone(),
                    p.getLocale(),
                    p.getBarkDeviceKey(),
                    p.getBio());
        }
    }

    public record CreateProfileRequest(
            String displayName,
            String email,
            String barkDeviceKey,
            String bio,
            String timezone,
            String locale) {
    }

    public record UpdateProfileRequest(
            String displayName,
            String email,
            String barkDeviceKey,
            String bio) {
    }
}
