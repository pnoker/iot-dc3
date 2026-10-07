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
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverDeviceStatusSummaryBO;
import io.github.pnoker.common.facade.entity.bo.FacadeSystemHealthBO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * System-health tool exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemTool {

    private final Optional<StatusHealthFacade> statusHealthFacade;

    /**
     * Return system health.
     *
     * @param toolContext tool context
     * @return get system health result
     */
    @Tool(
            description =
                    "Get a system health snapshot: center services, infrastructure, driver fleet, and device fleet.")
    @AgenticToolMetadata(domain = "system", title = "Get system health")
    public Mono<AgenticToolResult<FacadeSystemHealthBO>> getSystemHealth(ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "getSystemHealth", tenantId);
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE));
        }
        return facade.systemHealthReactive(tenantId)
                .map(health -> Objects.isNull(health)
                        ? AgenticToolResult.<FacadeSystemHealthBO>unavailable(
                                AgenticConstant.ToolMessage.SYSTEM_HEALTH_UNAVAILABLE)
                        : AgenticToolResult.ok("System health loaded", health));
    }

    /**
     * Return the online/offline status of the given devices.
     *
     * @param deviceIds device ids
     * @param toolContext tool context
     * @return device status map result
     */
    @Tool(description = "Get the online/offline status of devices by their IDs.")
    @AgenticToolMetadata(domain = "system", title = "Get device statuses by IDs")
    public Mono<AgenticToolResult<Map<Long, String>>> getDeviceStatuses(List<Long> deviceIds, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "getDeviceStatuses", tenantId);
        List<Long> ids = deviceIds == null
                ? List.of()
                : deviceIds.stream()
                        .filter(Objects::nonNull)
                        .filter(id -> id > 0)
                        .distinct()
                        .toList();
        if (ids.isEmpty()) {
            return Mono.just(AgenticToolResult.invalid("No valid device IDs provided."));
        }
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE));
        }
        return facade.listDeviceStatusesByIdsReactive(tenantId, ids)
                .map(statuses -> statuses == null || statuses.isEmpty()
                        ? AgenticToolResult.<Map<Long, String>>empty(
                                "No device statuses found for IDs: " + ids, Map.of())
                        : AgenticToolResult.ok("Device statuses loaded", statuses));
    }

    /**
     * Return the online/offline status of all devices under a profile.
     *
     * @param profileId profile id
     * @param toolContext tool context
     * @return device status map result
     */
    @Tool(description = "Get the online/offline status of all devices under a profile.")
    @AgenticToolMetadata(domain = "system", title = "Get device statuses by profile")
    public Mono<AgenticToolResult<Map<Long, String>>> getDeviceStatusesByProfile(
            Long profileId, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "getDeviceStatusesByProfile", tenantId);
        if (profileId == null || profileId <= 0) {
            return Mono.just(AgenticToolResult.invalid("Profile ID must be positive."));
        }
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE));
        }
        return facade.listDeviceStatusesByProfileIdReactive(tenantId, profileId)
                .map(statuses -> statuses == null || statuses.isEmpty()
                        ? AgenticToolResult.<Map<Long, String>>empty(
                                "No device statuses found for profile ID: " + profileId, Map.of())
                        : AgenticToolResult.ok("Device statuses loaded", statuses));
    }

    /**
     * Return the online/offline status of the given drivers.
     *
     * @param driverIds driver ids
     * @param toolContext tool context
     * @return driver status map result
     */
    @Tool(description = "Get the online/offline status of drivers by their IDs.")
    @AgenticToolMetadata(domain = "system", title = "Get driver statuses by IDs")
    public Mono<AgenticToolResult<Map<Long, String>>> getDriverStatuses(List<Long> driverIds, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "getDriverStatuses", tenantId);
        List<Long> ids = driverIds == null
                ? List.of()
                : driverIds.stream()
                        .filter(Objects::nonNull)
                        .filter(id -> id > 0)
                        .distinct()
                        .toList();
        if (ids.isEmpty()) {
            return Mono.just(AgenticToolResult.invalid("No valid driver IDs provided."));
        }
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE));
        }
        return facade.listDriverStatusesByIdsReactive(tenantId, ids)
                .map(statuses -> statuses == null || statuses.isEmpty()
                        ? AgenticToolResult.<Map<Long, String>>empty(
                                "No driver statuses found for IDs: " + ids, Map.of())
                        : AgenticToolResult.ok("Driver statuses loaded", statuses));
    }

    /**
     * Return the device status summary for one driver.
     *
     * @param driverId driver id
     * @param toolContext tool context
     * @return driver device status summary result
     */
    @Tool(description = "Get the online/offline device status summary for one driver: totals per status.")
    @AgenticToolMetadata(domain = "system", title = "Get driver device status summary")
    public Mono<AgenticToolResult<FacadeDriverDeviceStatusSummaryBO>> getDriverDeviceStatusSummary(
            Long driverId, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "getDriverDeviceStatusSummary", tenantId);
        if (driverId == null || driverId <= 0) {
            return Mono.just(AgenticToolResult.invalid("Driver ID must be positive."));
        }
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE));
        }
        return facade.getDriverDeviceStatusSummaryReactive(tenantId, driverId)
                .map(summary -> Objects.isNull(summary)
                        ? AgenticToolResult.<FacadeDriverDeviceStatusSummaryBO>notFound(
                                "No device status summary found for driver ID: " + driverId)
                        : AgenticToolResult.ok("Driver device status summary loaded", summary));
    }
}
