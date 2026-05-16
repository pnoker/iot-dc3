/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.agentic.tool;

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.ProfileTypeFlagEnum;
import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.bo.FacadeSystemHealthBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeProfileQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Platform-level tools for profiles/templates, fleet status, and health summaries.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2026.5.10
 */
@Slf4j
@Component
public class PlatformToolSet {

    private static final String PROFILE_UNAVAILABLE = "Profile tools are not available in this deployment mode.";

    private static final String STATUS_UNAVAILABLE = "Status and health tools are not available in this deployment mode.";

    private final Optional<ProfileFacade> profileFacade;

    private final Optional<StatusHealthFacade> statusHealthFacade;

    public PlatformToolSet(Optional<ProfileFacade> profileFacade, Optional<StatusHealthFacade> statusHealthFacade) {
        this.profileFacade = profileFacade;
        this.statusHealthFacade = statusHealthFacade;
    }

    @Tool(description = "Look up a profile/template by its numeric ID. Returns template name, code, type, share flag, enable status, and version.")
    public AgenticToolResult<FacadeProfileBO> lookupProfileById(
            @ToolParam(description = "The numeric profile/template ID") Long profileId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}", "lookupProfileById", tenantId,
                profileId);
        recordTool(toolContext, "lookupProfileById", "Query profile by ID");
        ProfileFacade facade = profileFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(PROFILE_UNAVAILABLE);
        }
        FacadeProfileBO profile = facade.selectById(tenantId, profileId);
        if (Objects.isNull(profile)) {
            return AgenticToolResult.notFound("Profile not found for ID: " + profileId);
        }
        return AgenticToolResult.ok("Profile loaded", profile);
    }

    @Tool(description = "Batch look up profiles/templates by numeric IDs. Returns up to 50 tenant-scoped templates.")
    public AgenticToolResult<List<FacadeProfileBO>> lookupProfilesByIds(
            @ToolParam(description = "The numeric profile/template IDs") List<Long> profileIds,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        List<Long> ids = normalizeIds(profileIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileIds={}", "lookupProfilesByIds", tenantId, ids);
        recordTool(toolContext, "lookupProfilesByIds", "Batch query profiles by IDs");
        ProfileFacade facade = profileFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(PROFILE_UNAVAILABLE);
        }
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid profile IDs provided.");
        }
        List<FacadeProfileBO> profiles = facade.selectByIds(tenantId, ids);
        if (Objects.isNull(profiles) || profiles.isEmpty()) {
            return AgenticToolResult.empty("No profiles found for IDs: " + ids, List.of());
        }
        return AgenticToolResult.ok("Profiles loaded", profiles);
    }

    @Tool(description = "Search profiles/templates with optional filters. profileType accepts system, driver, user, or their enum names.")
    public AgenticToolResult<FacadePage<FacadeProfileBO>> searchProfiles(
            @ToolParam(description = "Profile/template name filter (partial match), or null to skip") String profileName,
            @ToolParam(description = "Profile/template code filter, or null to skip") String profileCode,
            @ToolParam(description = "Profile type filter: system, driver, user, SYSTEM, DRIVER, USER, or null to skip") String profileType,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug(
                "Agentic tool invoked, tool={}, tenantId={}, profileName={}, profileCode={}, profileType={}, page={}, size={}",
                "searchProfiles", tenantId, profileName, profileCode, profileType, page, size);
        recordTool(toolContext, "searchProfiles", "Search profiles");
        ProfileFacade facade = profileFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(PROFILE_UNAVAILABLE);
        }

        FacadeProfileQuery query = new FacadeProfileQuery();
        query.setTenantId(tenantId);
        query.setProfileName(profileName);
        query.setProfileCode(profileCode);
        query.setProfileTypeFlag(parseProfileType(profileType));
        Pages p = new Pages();
        p.setCurrent(page);
        p.setSize(size);
        query.setPage(p);
        FacadePage<FacadeProfileBO> result = facade.selectByPage(query);
        if (Objects.isNull(result) || Objects.isNull(result.getRecords()) || result.getRecords().isEmpty()) {
            return AgenticToolResult.empty("No profiles found.", result);
        }
        return AgenticToolResult.ok("Profile page loaded", result);
    }

    @Tool(description = "List profiles/templates bound to a specific device ID.")
    public AgenticToolResult<List<FacadeProfileBO>> listProfilesByDeviceId(
            @ToolParam(description = "The device ID") Long deviceId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", "listProfilesByDeviceId", tenantId,
                deviceId);
        recordTool(toolContext, "listProfilesByDeviceId", "List profiles by device");
        ProfileFacade facade = profileFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(PROFILE_UNAVAILABLE);
        }
        List<FacadeProfileBO> profiles = facade.selectByDeviceId(tenantId, deviceId);
        if (Objects.isNull(profiles) || profiles.isEmpty()) {
            return AgenticToolResult.empty("No profiles found for device ID: " + deviceId, List.of());
        }
        return AgenticToolResult.ok("Profiles loaded for device " + deviceId, profiles);
    }

    @Tool(description = "Get device online/offline statuses for device IDs. Returns up to 50 tenant-scoped statuses.")
    public AgenticToolResult<Map<Long, String>> getDeviceStatusesByIds(
            @ToolParam(description = "The numeric device IDs") List<Long> deviceIds,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        List<Long> ids = normalizeIds(deviceIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceIds={}", "getDeviceStatusesByIds", tenantId, ids);
        recordTool(toolContext, "getDeviceStatusesByIds", "Get device statuses");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(STATUS_UNAVAILABLE);
        }
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid device IDs provided.");
        }
        Map<Long, String> statuses = facade.selectDeviceStatusesByIds(tenantId, ids);
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return AgenticToolResult.empty("No device statuses found.", Map.of());
        }
        return AgenticToolResult.ok("Device statuses loaded", statuses);
    }

    @Tool(description = "Get device online/offline statuses for devices bound to a profile/template.")
    public AgenticToolResult<Map<Long, String>> getDeviceStatusesByProfileId(
            @ToolParam(description = "The profile/template ID") Long profileId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}", "getDeviceStatusesByProfileId",
                tenantId, profileId);
        recordTool(toolContext, "getDeviceStatusesByProfileId", "Get device statuses by profile");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(STATUS_UNAVAILABLE);
        }
        Map<Long, String> statuses = facade.selectDeviceStatusesByProfileId(tenantId, profileId);
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return AgenticToolResult.empty("No device statuses found for profile ID: " + profileId, Map.of());
        }
        return AgenticToolResult.ok("Device statuses loaded for profile " + profileId, statuses);
    }

    @Tool(description = "Get driver online/offline statuses for driver IDs. Returns up to 50 tenant-scoped statuses.")
    public AgenticToolResult<Map<Long, String>> getDriverStatusesByIds(
            @ToolParam(description = "The numeric driver IDs") List<Long> driverIds,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        List<Long> ids = normalizeIds(driverIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverIds={}", "getDriverStatusesByIds", tenantId, ids);
        recordTool(toolContext, "getDriverStatusesByIds", "Get driver statuses");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(STATUS_UNAVAILABLE);
        }
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid driver IDs provided.");
        }
        Map<Long, String> statuses = facade.selectDriverStatusesByIds(tenantId, ids);
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return AgenticToolResult.empty("No driver statuses found.", Map.of());
        }
        return AgenticToolResult.ok("Driver statuses loaded", statuses);
    }

    @Tool(description = "Get the online/offline device count summary under a driver.")
    public AgenticToolResult<Map<String, String>> getDriverDeviceStatusSummary(
            @ToolParam(description = "The driver ID") Long driverId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverId={}", "getDriverDeviceStatusSummary", tenantId,
                driverId);
        recordTool(toolContext, "getDriverDeviceStatusSummary", "Get driver device status summary");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(STATUS_UNAVAILABLE);
        }
        Map<String, String> summary = facade.getDriverDeviceStatusSummary(tenantId, driverId);
        if (Objects.isNull(summary) || summary.isEmpty()) {
            return AgenticToolResult.empty("No driver device status summary found for driver ID: " + driverId,
                    Map.of());
        }
        return AgenticToolResult.ok("Driver device status summary loaded", summary);
    }

    @Tool(description = "Get a system health snapshot: center services, infrastructure, driver fleet, and device fleet.")
    public AgenticToolResult<FacadeSystemHealthBO> getSystemHealth(ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "getSystemHealth", tenantId);
        recordTool(toolContext, "getSystemHealth", "Get system health");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(STATUS_UNAVAILABLE);
        }
        FacadeSystemHealthBO health = facade.systemHealth(tenantId);
        if (Objects.isNull(health)) {
            return AgenticToolResult.unavailable("System health snapshot is unavailable.");
        }
        return AgenticToolResult.ok("System health loaded", health);
    }

    private void recordTool(ToolContext toolContext, String toolName, String description) {
        AgenticRequestContext.recordToolInvocation(toolContext, toolName, "platform", description);
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().limit(50).toList();
    }

    private ProfileTypeFlagEnum parseProfileType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return ProfileTypeFlagEnum.ofIndex(Byte.valueOf(trimmed));
        } catch (NumberFormatException ignored) {
            // Continue with code/name lookup.
        }
        ProfileTypeFlagEnum byCode = ProfileTypeFlagEnum.ofCode(trimmed.toLowerCase());
        return Objects.nonNull(byCode) ? byCode : ProfileTypeFlagEnum.ofName(trimmed.toUpperCase());
    }

}
