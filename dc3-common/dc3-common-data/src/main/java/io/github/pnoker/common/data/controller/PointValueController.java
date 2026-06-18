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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.entity.vo.PointValueVO;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
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

import java.util.List;
import java.util.Objects;

/**
 * REST controller exposing point value management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "point_value", description = "Data point values: query real-time snapshots and historical time-series values collected from industrial device data points")
@Slf4j
@RestController
@RequestMapping(DataConstant.POINT_VALUE_URL_PREFIX)
@RequiredArgsConstructor
public class PointValueController implements BaseController {

    private final PointValueBuilder pointValueBuilder;

    private final PointValueService pointValueService;

    /**
     * Query the latest point value for each point in the device.
     *
     * @param entityQuery PointValueQuery, including pagination parameters
     * @return Page of PointValueVO, where each PointValueVO contains the latest value for
     * a point in the device
     */
    @PreAuthorize("@perm.can('point_value', 'list')")
    @Operation(summary = "List Latest Point Values", description = "Return the most recent reading for each point under a device for the current tenant, " +
            "paged by the request query. Use to read near-real-time snapshots; results are ordered by collection time.")
    @PostMapping("/latest")
    public Mono<R<Page<PointValueVO>>> latest(@RequestBody PointValueQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointValueQuery query = Objects.isNull(entityQuery) ? new PointValueQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<PointValueBO> entityPageBO = pointValueService.latest(query);
            Page<PointValueVO> entityPageVO = pointValueBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Query historical point values for each point in the device.
     *
     * @param entityQuery PointValueQuery, including pagination parameters
     * @return Page of PointValueVO, where each PointValueVO contains the historical value
     * for a point in the device
     */
    @PreAuthorize("@perm.can('point_value', 'list')")
    @Operation(summary = "List Point Values", description = "Page through stored time-series readings for points under a device (tenant-scoped). " +
            "Use to query raw historical values; results are ordered by collection time.")
    @PostMapping("/list")
    public Mono<R<Page<PointValueVO>>> list(@RequestBody(required = false) PointValueQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointValueQuery query = Objects.isNull(entityQuery) ? new PointValueQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<PointValueBO> entityPageBO = pointValueService.page(query);
            Page<PointValueVO> entityPageVO = pointValueBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Query historical point values for a specific point in the device.
     *
     * @param deviceId Device ID
     * @param pointId  Point ID
     * @return List of String, where each String is the historical value for the point
     */
    @PreAuthorize("@perm.can('point_value', 'list')")
    @Operation(summary = "List Point Value History by Device and Point", description = "Return the most recent time-series values for one point on one device for the current tenant, " +
            "as a list of raw value strings bounded by count (default 100). Use to read a single point's latest history.")
    @GetMapping("/list_history_by_device_id_and_point_id")
    public Mono<R<List<String>>> history(@Parameter(description = "Device ID") @NotNull @RequestParam(name = "device_id") Long deviceId,
                                         @Parameter(description = "Point ID") @NotNull @RequestParam(name = "point_id") Long pointId,
                                         @Parameter(description = "Maximum number of historical values to return")
                                         @RequestParam(name = "count", required = false, defaultValue = "100") Integer count) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<String> history = pointValueService.history(tenantId, deviceId, pointId, count);
            return R.ok(history);
        }));
    }

}
