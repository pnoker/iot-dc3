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

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.agentic.utils.AgenticToolUtil;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.query.FacadeProfileOffsetQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Non-blocking profile/template tools. */
@Component
@RequiredArgsConstructor
public class ProfileTool {
    private final Optional<ProfileFacade> profileFacade;

    /** Look up the profile by id. */
    public Mono<AgenticToolResult<FacadeProfileBO>> lookupProfileByIdReactive(Long profileId, ToolContext context) {
        return Mono.defer(() -> {
            Long tenantId = AgenticToolContextUtil.requireTenantId(context);
            ProfileFacade facade = profileFacade.orElse(null);
            if (facade == null)
                return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.PROFILE_UNAVAILABLE));
            if (profileId == null || profileId <= 0)
                return Mono.just(AgenticToolResult.invalid("Profile ID must be positive."));
            return facade.getByIdReactive(tenantId, profileId)
                    .map(value -> AgenticToolResult.ok("Profile loaded", value))
                    .defaultIfEmpty(AgenticToolResult.notFound("Profile not found for ID: " + profileId));
        });
    }

    /** Look up the profiles for the given ids. */
    public Mono<AgenticToolResult<List<FacadeProfileBO>>> lookupProfilesByIdsReactive(
            List<Long> profileIds, ToolContext context) {
        return Mono.defer(() -> {
            Long tenantId = AgenticToolContextUtil.requireTenantId(context);
            ProfileFacade facade = profileFacade.orElse(null);
            if (facade == null)
                return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.PROFILE_UNAVAILABLE));
            List<Long> ids = AgenticToolUtil.normalizeIds(profileIds);
            if (ids.isEmpty()) return Mono.just(AgenticToolResult.invalid("No valid profile IDs provided."));
            return facade.listByIdsReactive(tenantId, ids)
                    .collectList()
                    .map(values -> values.isEmpty()
                            ? AgenticToolResult.empty("No profiles found for IDs: " + ids, List.of())
                            : AgenticToolResult.ok("Profiles loaded", values));
        });
    }

    /** Search profiles matching the request. */
    public Mono<AgenticToolResult<OffsetPage<FacadeProfileBO>>> searchProfilesReactive(
            String profileName, String profileCode, String profileType, long offset, int limit, ToolContext context) {
        return Mono.defer(() -> {
            Long tenantId = AgenticToolContextUtil.requireTenantId(context);
            ProfileFacade facade = profileFacade.orElse(null);
            if (facade == null)
                return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.PROFILE_UNAVAILABLE));
            if (offset < 0) return Mono.just(AgenticToolResult.invalid("Offset must be non-negative."));
            if (limit < 1 || limit > 200)
                return Mono.just(AgenticToolResult.invalid("Limit must be between 1 and 200."));
            return facade.listReactive(new FacadeProfileOffsetQuery(
                            tenantId,
                            profileName,
                            profileCode,
                            null,
                            parseProfileType(profileType),
                            null,
                            null,
                            null,
                            null,
                            null,
                            offset,
                            limit,
                            List.of()))
                    .map(page -> page.items().isEmpty()
                            ? AgenticToolResult.empty("No profiles found.", page)
                            : AgenticToolResult.ok("Profile page loaded", page));
        });
    }

    /** List profile tools matched by device id. */
    public Mono<AgenticToolResult<OffsetPage<FacadeProfileBO>>> listProfilesByDeviceIdReactive(
            Long deviceId, long offset, int limit, ToolContext context) {
        return Mono.defer(() -> {
            Long tenantId = AgenticToolContextUtil.requireTenantId(context);
            ProfileFacade facade = profileFacade.orElse(null);
            if (facade == null)
                return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.PROFILE_UNAVAILABLE));
            if (deviceId == null || deviceId <= 0)
                return Mono.just(AgenticToolResult.invalid("Device ID must be positive."));
            if (offset < 0) return Mono.just(AgenticToolResult.invalid("Offset must be non-negative."));
            if (limit < 1 || limit > 200)
                return Mono.just(AgenticToolResult.invalid("Limit must be between 1 and 200."));
            return facade.listReactive(new FacadeProfileOffsetQuery(
                            tenantId, null, null, null, null, null, null, null, null, deviceId, offset, limit,
                            List.of()))
                    .map(page -> page.items().isEmpty()
                            ? AgenticToolResult.empty("No profiles found for device ID: " + deviceId, page)
                            : AgenticToolResult.ok("Profile page loaded for device " + deviceId, page));
        });
    }

    private ProfileTypeEnum parseProfileType(String value) {
        if (StringUtils.isBlank(value)) return null;
        String trimmed = value.trim();
        try {
            return ProfileTypeEnum.ofIndex(Byte.valueOf(trimmed));
        } catch (NumberFormatException ignored) {
            ProfileTypeEnum byCode = ProfileTypeEnum.ofCode(trimmed.toLowerCase());
            return byCode == null ? ProfileTypeEnum.ofName(trimmed.toUpperCase()) : byCode;
        }
    }
}
