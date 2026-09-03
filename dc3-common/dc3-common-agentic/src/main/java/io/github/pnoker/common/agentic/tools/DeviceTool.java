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
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointOffsetQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Non-blocking device metadata and latest-value tools. */
@Component
@RequiredArgsConstructor
public class DeviceTool {
    private final DeviceFacade deviceFacade;
    private final PointFacade pointFacade;
    private final PointValueFacade pointValueFacade;

    /** Look up the device by id. */
    public Mono<AgenticToolResult<FacadeDeviceBO>> lookupDeviceByIdReactive(Long deviceId, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        if (deviceId == null || deviceId <= 0)
            return Mono.just(AgenticToolResult.invalid("Device ID must be positive."));
        return deviceFacade
                .getByIdReactive(tenantId, deviceId)
                .map(value -> AgenticToolResult.ok("Device loaded", value))
                .defaultIfEmpty(AgenticToolResult.notFound("Device not found for ID: " + deviceId));
    }

    /** Look up the devices for the given ids. */
    public Mono<AgenticToolResult<List<FacadeDeviceBO>>> lookupDevicesByIdsReactive(
            List<Long> deviceIds, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        List<Long> ids = AgenticToolUtil.normalizeIds(deviceIds);
        if (ids.isEmpty()) return Mono.just(AgenticToolResult.invalid("No valid device IDs provided."));
        return deviceFacade
                .listByIdsReactive(tenantId, ids)
                .collectList()
                .map(values -> values.isEmpty()
                        ? AgenticToolResult.empty("No devices found for IDs: " + ids, List.of())
                        : AgenticToolResult.ok("Devices loaded", values));
    }

    /** Search devices matching the request. */
    public Mono<AgenticToolResult<OffsetPage<FacadeDeviceBO>>> searchDevicesReactive(
            String deviceName, String deviceCode, Long driverId, long offset, int limit, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        if (offset < 0) return Mono.just(AgenticToolResult.invalid("Offset must be non-negative."));
        if (limit < 1 || limit > 200) return Mono.just(AgenticToolResult.invalid("Limit must be between 1 and 200."));
        return deviceFacade
                .listReactive(new FacadeDeviceOffsetQuery(
                        tenantId,
                        deviceName,
                        deviceCode,
                        driverId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        offset,
                        limit,
                        List.of()))
                .map(page -> page.items().isEmpty()
                        ? AgenticToolResult.empty("No devices found.", page)
                        : AgenticToolResult.ok("Device page loaded", page));
    }

    /** List device tools matched by driver id. */
    public Mono<AgenticToolResult<OffsetPage<FacadeDeviceBO>>> listDevicesByDriverIdReactive(
            Long driverId, long offset, int limit, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        if (driverId == null || driverId <= 0)
            return Mono.just(AgenticToolResult.invalid("Driver ID must be positive."));
        if (offset < 0) return Mono.just(AgenticToolResult.invalid("Offset must be non-negative."));
        if (limit < 1 || limit > 200) return Mono.just(AgenticToolResult.invalid("Limit must be between 1 and 200."));
        return deviceFacade
                .listReactive(new FacadeDeviceOffsetQuery(
                        tenantId, null, null, driverId, null, null, null, null, null, offset, limit, List.of()))
                .map(page -> page.items().isEmpty()
                        ? AgenticToolResult.empty("No devices found for driver ID: " + driverId, page)
                        : AgenticToolResult.ok("Device page loaded for driver " + driverId, page));
    }

    /** List device tools matched by profile id. */
    public Mono<AgenticToolResult<OffsetPage<FacadeDeviceBO>>> listDevicesByProfileIdReactive(
            Long profileId, long offset, int limit, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        if (profileId == null || profileId <= 0)
            return Mono.just(AgenticToolResult.invalid("Profile ID must be positive."));
        if (offset < 0) return Mono.just(AgenticToolResult.invalid("Offset must be non-negative."));
        if (limit < 1 || limit > 200) return Mono.just(AgenticToolResult.invalid("Limit must be between 1 and 200."));
        return deviceFacade
                .listReactive(new FacadeDeviceOffsetQuery(
                        tenantId, null, null, null, profileId, null, null, null, null, offset, limit, List.of()))
                .map(page -> page.items().isEmpty()
                        ? AgenticToolResult.empty("No devices found for profile ID: " + profileId, page)
                        : AgenticToolResult.ok("Device page loaded for profile " + profileId, page));
    }

    /** Load the device latest point values for the request. */
    public Mono<AgenticToolResult<DeviceLatestPointValues>> getDeviceLatestPointValuesReactive(
            Long deviceId, int limit, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        if (deviceId == null || deviceId <= 0)
            return Mono.just(AgenticToolResult.invalid("Device ID must be positive."));
        if (limit < 1 || limit > AgenticConstant.ToolLimit.MAX_IDS) {
            return Mono.just(AgenticToolResult.invalid(
                    "Limit must be between 1 and " + AgenticConstant.ToolLimit.MAX_IDS + "."));
        }
        int safeLimit = limit;
        return deviceFacade
                .getByIdReactive(tenantId, deviceId)
                .flatMap(device -> pointFacade
                        .listReactive(new FacadePointOffsetQuery(
                                tenantId, null, null, null, null, null, null, null, null, null, deviceId, 0, safeLimit,
                                List.of()))
                        .flatMapMany(page -> Flux.fromIterable(page.items()))
                        .flatMap(
                                point -> pointValueFacade
                                        .lastValue(tenantId, deviceId, point.getId())
                                        .map(value -> new PointLatestValue(point, value))
                                        .defaultIfEmpty(new PointLatestValue(point, null)),
                                8)
                        .collectList()
                        .map(values -> values.isEmpty()
                                ? AgenticToolResult.empty(
                                        "No points found for device " + deviceId,
                                        new DeviceLatestPointValues(device, List.of()))
                                : AgenticToolResult.ok(
                                        "Device latest point values loaded",
                                        new DeviceLatestPointValues(device, values))))
                .defaultIfEmpty(AgenticToolResult.notFound("Device not found for ID: " + deviceId));
    }

    public record DeviceLatestPointValues(FacadeDeviceBO device, List<PointLatestValue> points) {
        public DeviceLatestPointValues {
            points = List.copyOf(points == null ? List.of() : points);
        }
    }

    public record PointLatestValue(FacadePointBO point, FacadePointValueBO value) {}
}
