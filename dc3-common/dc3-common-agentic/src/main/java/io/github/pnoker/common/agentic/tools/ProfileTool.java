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
package io.github.pnoker.common.agentic.tools;

import io.github.pnoker.common.agentic.annotation.AgenticToolMetadata;
import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.agentic.utils.AgenticToolUtil;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.enums.ProfileTypeFlagEnum;
import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeProfileQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Profile/template-domain tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileTool {

    private final Optional<ProfileFacade> profileFacade;

    @Tool(description = "Look up a profile/template by its numeric ID. Returns template name, code, type, share flag, enable status, and version.")
    @AgenticToolMetadata(domain = "profile", title = "Query profile by ID")
    public AgenticToolResult<FacadeProfileBO> lookupProfileById(
            @ToolParam(description = "The numeric profile/template ID") Long profileId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}", "lookupProfileById", tenantId,
                profileId);
        ProfileFacade facade = profileFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(AgenticConstant.ToolMessage.PROFILE_UNAVAILABLE);
        }
        FacadeProfileBO profile = facade.getById(tenantId, profileId);
        if (Objects.isNull(profile)) {
            return AgenticToolResult.notFound("Profile not found for ID: " + profileId);
        }
        return AgenticToolResult.ok("Profile loaded", profile);
    }

    @Tool(description = "Batch look up profiles/templates by numeric IDs. Returns up to 50 tenant-scoped templates.")
    @AgenticToolMetadata(domain = "profile", title = "Batch query profiles by IDs")
    public AgenticToolResult<List<FacadeProfileBO>> lookupProfilesByIds(
            @ToolParam(description = "The numeric profile/template IDs") List<Long> profileIds,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        List<Long> ids = AgenticToolUtil.normalizeIds(profileIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileIds={}", "lookupProfilesByIds", tenantId, ids);
        ProfileFacade facade = profileFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(AgenticConstant.ToolMessage.PROFILE_UNAVAILABLE);
        }
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid profile IDs provided.");
        }
        List<FacadeProfileBO> profiles = facade.listByIds(tenantId, ids);
        if (Objects.isNull(profiles) || profiles.isEmpty()) {
            return AgenticToolResult.empty("No profiles found for IDs: " + ids, List.of());
        }
        return AgenticToolResult.ok("Profiles loaded", profiles);
    }

    @Tool(description = "Search profiles/templates with optional filters. profileType accepts system, driver, user, or their enum names.")
    @AgenticToolMetadata(domain = "profile", title = "Search profiles")
    public AgenticToolResult<FacadePage<FacadeProfileBO>> searchProfiles(
            @ToolParam(description = "Profile/template name filter (partial match), or null to skip") String profileName,
            @ToolParam(description = "Profile/template code filter, or null to skip") String profileCode,
            @ToolParam(description = "Profile type filter: system, driver, user, SYSTEM, DRIVER, USER, or null to skip") String profileType,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug(
                "Agentic tool invoked, tool={}, tenantId={}, profileName={}, profileCode={}, profileType={}, page={}, size={}",
                "searchProfiles", tenantId, profileName, profileCode, profileType, page, size);
        ProfileFacade facade = profileFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(AgenticConstant.ToolMessage.PROFILE_UNAVAILABLE);
        }

        FacadeProfileQuery query = new FacadeProfileQuery();
        query.setTenantId(tenantId);
        query.setProfileName(profileName);
        query.setProfileCode(profileCode);
        query.setProfileTypeFlag(parseProfileType(profileType));
        query.setPage(AgenticToolUtil.page(page, size));
        FacadePage<FacadeProfileBO> result = facade.listByPage(query);
        if (!AgenticToolUtil.hasRecords(result)) {
            return AgenticToolResult.empty("No profiles found.", result);
        }
        return AgenticToolResult.ok("Profile page loaded", result);
    }

    @Tool(description = "List profiles/templates bound to a specific device ID.")
    @AgenticToolMetadata(domain = "profile", title = "List profiles by device")
    public AgenticToolResult<List<FacadeProfileBO>> listProfilesByDeviceId(
            @ToolParam(description = "The device ID") Long deviceId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", "listProfilesByDeviceId", tenantId,
                deviceId);
        ProfileFacade facade = profileFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(AgenticConstant.ToolMessage.PROFILE_UNAVAILABLE);
        }
        List<FacadeProfileBO> profiles = facade.listByDeviceId(tenantId, deviceId);
        if (Objects.isNull(profiles) || profiles.isEmpty()) {
            return AgenticToolResult.empty("No profiles found for device ID: " + deviceId, List.of());
        }
        return AgenticToolResult.ok("Profiles loaded for device " + deviceId, profiles);
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
