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
import io.github.pnoker.common.data.biz.DriverStatusService;
import io.github.pnoker.common.data.entity.bo.DriverRunBO;
import io.github.pnoker.common.data.entity.builder.DriverDurationBuilder;
import io.github.pnoker.common.data.entity.query.DriverQuery;
import io.github.pnoker.common.data.entity.vo.DriverRunVO;
import io.github.pnoker.common.entity.R;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * 设备 Controller
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.DRIVER_STATUS_URL_PREFIX)
public class DriverStatusController implements BaseController {

    private final DriverStatusService driverStatusService;
    private final DriverDurationBuilder driverDurationBuilder;

    public DriverStatusController(DriverStatusService driverStatusService, DriverDurationBuilder driverDurationBuilder) {
        this.driverStatusService = driverStatusService;
        this.driverDurationBuilder = driverDurationBuilder;
    }

    /**
     * 查询驱动状态
     * ONLINE, OFFLINE
     *
     * @param entityQuery 驱动和分页参数
     * @return Map String:String
     */
    @PostMapping("/driver")
    public Mono<R<Map<Long, String>>> driverStatus(@RequestBody(required = false) DriverQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DriverQuery query = Objects.isNull(entityQuery) ? new DriverQuery() : entityQuery;
                query.setTenantId(tenantId);
                Map<Long, String> statuses = driverStatusService.selectByPage(query);
                return Mono.just(R.ok(statuses));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 查询 Driver 在线总时长
     *
     * @param driverId
     * @return
     */
    @GetMapping("/driverOnline/{driverId}")
    public Mono<R<DriverRunVO>> selectOnlineByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            DriverRunBO duration = driverStatusService.selectOnlineByDriverId(driverId);
            DriverRunVO result = driverDurationBuilder.buildVOByBOList(duration);
            return Mono.just(R.ok(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 查询 Driver 离线总时长
     *
     * @param driverId
     * @return
     */
    @GetMapping("/driverOffline/{driverId}")
    public Mono<R<DriverRunVO>> selectOfflineByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            DriverRunBO duration = driverStatusService.selectOfflineByDriverId(driverId);
            DriverRunVO result = driverDurationBuilder.buildVOByBOList(duration);
            return Mono.just(R.ok(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 查询 Driver 下当前时刻在线设备数量
     *
     * @param driverId
     * @return
     */
    @GetMapping("/getDeviceOnlineByDriverId/{driverId}")
    public Mono<R<String>> getDeviceOnlineByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            String result = driverStatusService.getDeviceOnlineByDriverId(driverId);
            return Mono.just(R.ok(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 查询 Driver 下当前时刻离线设备数量
     *
     * @param driverId
     * @return
     */
    @GetMapping("/getDeviceOfflineByDriverId/{driverId}")
    public Mono<R<String>> getDeviceOfflineByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            String result = driverStatusService.getDeviceOfflineByDriverId(driverId);
            return Mono.just(R.ok(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
