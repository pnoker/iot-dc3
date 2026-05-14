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
import io.github.pnoker.common.enums.ProfileShareFlagEnum;
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
import java.util.stream.Collectors;

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
    public String lookupProfileById(@ToolParam(description = "The numeric profile/template ID") Long profileId,
                                    ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}", "lookupProfileById", tenantId,
                profileId);
        recordTool(toolContext, "lookupProfileById", "Query profile by ID");
        ProfileFacade facade = profileFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return PROFILE_UNAVAILABLE;
        }
        FacadeProfileBO profile = facade.selectById(tenantId, profileId);
        return Objects.isNull(profile) ? "Profile not found for ID: " + profileId : formatProfile(profile);
    }

    @Tool(description = "Batch look up profiles/templates by numeric IDs. Returns up to 50 tenant-scoped templates.")
    public String lookupProfilesByIds(@ToolParam(description = "The numeric profile/template IDs") List<Long> profileIds,
                                      ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        List<Long> ids = normalizeIds(profileIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileIds={}", "lookupProfilesByIds", tenantId, ids);
        recordTool(toolContext, "lookupProfilesByIds", "Batch query profiles by IDs");
        ProfileFacade facade = profileFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return PROFILE_UNAVAILABLE;
        }
        if (ids.isEmpty()) {
            return "No valid profile IDs provided.";
        }
        List<FacadeProfileBO> profiles = facade.selectByIds(tenantId, ids);
        if (profiles.isEmpty()) {
            return "No profiles found for IDs: " + ids;
        }
        return profiles.stream().map(this::formatProfile).collect(Collectors.joining("\n"));
    }

    @Tool(description = "Search profiles/templates with optional filters. profileType accepts system, driver, user, or their enum names.")
    public String searchProfiles(
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
            return PROFILE_UNAVAILABLE;
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
        return formatProfilePage(facade.selectByPage(query));
    }

    @Tool(description = "List profiles/templates bound to a specific device ID.")
    public String listProfilesByDeviceId(@ToolParam(description = "The device ID") Long deviceId,
                                         ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", "listProfilesByDeviceId", tenantId,
                deviceId);
        recordTool(toolContext, "listProfilesByDeviceId", "List profiles by device");
        ProfileFacade facade = profileFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return PROFILE_UNAVAILABLE;
        }
        List<FacadeProfileBO> profiles = facade.selectByDeviceId(tenantId, deviceId);
        if (profiles.isEmpty()) {
            return "No profiles found for device ID: " + deviceId;
        }
        return profiles.stream().map(this::formatProfile).collect(Collectors.joining("\n"));
    }

    @Tool(description = "Get device online/offline statuses for device IDs. Returns up to 50 tenant-scoped statuses.")
    public String getDeviceStatusesByIds(@ToolParam(description = "The numeric device IDs") List<Long> deviceIds,
                                         ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        List<Long> ids = normalizeIds(deviceIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceIds={}", "getDeviceStatusesByIds", tenantId, ids);
        recordTool(toolContext, "getDeviceStatusesByIds", "Get device statuses");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return STATUS_UNAVAILABLE;
        }
        if (ids.isEmpty()) {
            return "No valid device IDs provided.";
        }
        return formatStatusMap("Device statuses", facade.selectDeviceStatusesByIds(tenantId, ids));
    }

    @Tool(description = "Get device online/offline statuses for devices bound to a profile/template.")
    public String getDeviceStatusesByProfileId(@ToolParam(description = "The profile/template ID") Long profileId,
                                               ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}", "getDeviceStatusesByProfileId",
                tenantId, profileId);
        recordTool(toolContext, "getDeviceStatusesByProfileId", "Get device statuses by profile");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return STATUS_UNAVAILABLE;
        }
        return formatStatusMap("Device statuses for profile " + profileId,
                facade.selectDeviceStatusesByProfileId(tenantId, profileId));
    }

    @Tool(description = "Get driver online/offline statuses for driver IDs. Returns up to 50 tenant-scoped statuses.")
    public String getDriverStatusesByIds(@ToolParam(description = "The numeric driver IDs") List<Long> driverIds,
                                         ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        List<Long> ids = normalizeIds(driverIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverIds={}", "getDriverStatusesByIds", tenantId, ids);
        recordTool(toolContext, "getDriverStatusesByIds", "Get driver statuses");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return STATUS_UNAVAILABLE;
        }
        if (ids.isEmpty()) {
            return "No valid driver IDs provided.";
        }
        return formatStatusMap("Driver statuses", facade.selectDriverStatusesByIds(tenantId, ids));
    }

    @Tool(description = "Get the online/offline device count summary under a driver.")
    public String getDriverDeviceStatusSummary(@ToolParam(description = "The driver ID") Long driverId,
                                               ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverId={}", "getDriverDeviceStatusSummary", tenantId,
                driverId);
        recordTool(toolContext, "getDriverDeviceStatusSummary", "Get driver device status summary");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return STATUS_UNAVAILABLE;
        }
        Map<String, String> summary = facade.getDriverDeviceStatusSummary(tenantId, driverId);
        if (summary.isEmpty()) {
            return "No driver device status summary found for driver ID: " + driverId;
        }
        return summary.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    @Tool(description = "Get a system health snapshot: center services, infrastructure, driver fleet, and device fleet.")
    public String getSystemHealth(ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "getSystemHealth", tenantId);
        recordTool(toolContext, "getSystemHealth", "Get system health");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return STATUS_UNAVAILABLE;
        }
        FacadeSystemHealthBO health = facade.systemHealth(tenantId);
        if (Objects.isNull(health)) {
            return "System health snapshot is unavailable.";
        }
        return formatHealth(health);
    }

    private void recordTool(ToolContext toolContext, String toolName, String description) {
        AgenticRequestContext.recordToolInvocation(toolContext, toolName, "platform", description);
    }

    private String formatProfile(FacadeProfileBO profile) {
        return String.format(
                "Profile[id=%d, name=%s, code=%s, share=%s, type=%s, enabled=%s, version=%s]",
                profile.getId(), profile.getProfileName(), profile.getProfileCode(), profile.getProfileShareFlag(),
                profile.getProfileTypeFlag(), profile.getEnableFlag(), profile.getVersion());
    }

    private String formatProfilePage(FacadePage<FacadeProfileBO> page) {
        if (Objects.isNull(page) || page.getRecords().isEmpty()) {
            return "No profiles found.";
        }
        String items = page.getRecords().stream().map(this::formatProfile).collect(Collectors.joining("\n"));
        return String.format("Page %d/%d (total %d):\n%s", page.getCurrent(), page.getPages(), page.getTotal(), items);
    }

    private String formatStatusMap(String title, Map<Long, String> statuses) {
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return title + ": no statuses found.";
        }
        String items = statuses.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
        return title + ": " + items;
    }

    private String formatHealth(FacadeSystemHealthBO health) {
        return String.format("Health[center=%s, infra=%s, drivers=%s/%s online, devices=%s/%s online]",
                health.getCenter(), health.getInfra(), online(health.getDrivers()), total(health.getDrivers()),
                online(health.getDevices()), total(health.getDevices()));
    }

    private int total(FacadeSystemHealthBO.FleetSummary summary) {
        return Objects.nonNull(summary) ? summary.getTotal() : 0;
    }

    private int online(FacadeSystemHealthBO.FleetSummary summary) {
        return Objects.nonNull(summary) ? summary.getOnline() : 0;
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

    @SuppressWarnings("unused")
    private ProfileShareFlagEnum parseProfileShare(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return ProfileShareFlagEnum.ofIndex(Byte.valueOf(trimmed));
        } catch (NumberFormatException ignored) {
            // Continue with code/name lookup.
        }
        ProfileShareFlagEnum byCode = ProfileShareFlagEnum.ofCode(trimmed.toLowerCase());
        return Objects.nonNull(byCode) ? byCode : ProfileShareFlagEnum.ofName(trimmed.toUpperCase());
    }

}
