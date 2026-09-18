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
package io.github.pnoker.common.data.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.entity.vo.PointValueVO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing point value management endpoints.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(
        name = "point_value",
        description =
                "Data point values: query real-time snapshots and historical time-series values collected from industrial device data points")
@Slf4j
@RestController
@RequestMapping(DataConstant.POINT_VALUE_URL_PREFIX)
@RequiredArgsConstructor
public class PointValueController implements BaseController {

    private final PointValueBuilder pointValueBuilder;

    private final PointValueService pointValueService;

    /**
     * Return the most recent reading for each point under a device.
     *
     * @param entityQuery point value query carrying device scope and pagination parameters
     * @return a page of PointValueVO, where each entry holds the latest value of one point
     */
    @PreAuthorize("@perm.can('point_value', 'list')")
    @Operation(
            summary = "List Latest Point Values",
            description =
                    "Return the most recent reading for each point under a device for the current tenant, "
                            + "paged by the request query. Use to read near-real-time snapshots; results are ordered by collection time.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/latest")
    public Mono<OffsetPage<PointValueVO>> latest(@RequestBody(required = false) PointValueQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            PointValueQuery query = Objects.isNull(entityQuery) ? new PointValueQuery() : entityQuery;
            query.setTenantId(tenantId);
            return pointValueService
                    .latest(query)
                    .map(page -> OffsetPage.of(
                            page.items().stream()
                                    .map(pointValueBuilder::buildVOByBO)
                                    .toList(),
                            page.offset(),
                            page.limit(),
                            page.total()));
        });
    }

    /**
     * Page through stored time-series readings for points under a device.
     *
     * @param entityQuery point value query carrying device scope and pagination parameters; treated as empty when null
     * @return a page of PointValueVO matching the query, ordered by collection time
     */
    @PreAuthorize("@perm.can('point_value', 'list')")
    @Operation(
            summary = "List Point Values",
            description = "Page through stored time-series readings for points under a device (tenant-scoped). "
                    + "Use to query raw historical values; results are ordered by collection time.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list")
    public Mono<CursorPage<PointValueVO>> list(@RequestBody(required = false) PointValueQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            PointValueQuery query = Objects.isNull(entityQuery) ? new PointValueQuery() : entityQuery;
            query.setTenantId(tenantId);
            return pointValueService
                    .page(query)
                    .map(page -> CursorPage.of(
                            page.items().stream()
                                    .map(pointValueBuilder::buildVOByBO)
                                    .toList(),
                            page.nextCursor()));
        });
    }

    /**
     * Return the most recent time-series values for a single point on a device.
     *
     * @param deviceId id of the device whose point history is being queried
     * @param pointId  id of the point whose history is being queried
     * @param cursor   opaque cursor returned by the previous page
     * @param limit    maximum number of historical values to return; defaults to 100 when omitted
     * @return a list of raw value strings for the point, bounded by limit
     */
    @PreAuthorize("@perm.can('point_value', 'list')")
    @Operation(
            summary = "List Point Value History by Device and Point",
            description =
                    "Return the most recent time-series values for one point on one device for the current tenant, "
                            + "as a list of raw value strings bounded by limit (default 100). Use to read a single point's latest history.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/history")
    public Mono<CursorPage<PointValueVO>> history(
            @Parameter(description = "Identifier of the device; must belong to the current tenant", example = "1024")
                    @NotNull
                    @RequestParam(name = "device_id")
                    Long deviceId,
            @Parameter(description = "Identifier of the point; must belong to the device profile", example = "2048")
                    @NotNull
                    @RequestParam(name = "point_id")
                    Long pointId,
            @Parameter(description = "Opaque cursor returned by the previous page")
                    @RequestParam(name = "cursor", required = false)
                    String cursor,
            @Parameter(description = "Page size from 1 through 500", example = "100")
                    @RequestParam(name = "limit", required = false, defaultValue = "100")
                    Integer limit) {
        return getTenantId()
                .flatMap(tenantId -> pointValueService
                        .history(tenantId, deviceId, pointId, cursor, limit)
                        .map(page -> CursorPage.of(
                                page.items().stream()
                                        .map(pointValueBuilder::buildVOByBO)
                                        .toList(),
                                page.nextCursor())));
    }
}
