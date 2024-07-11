/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * 设备 Controller
 *
 * @author pnoker
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
     * @param driverQuery 驱动和分页参数
     * @return Map String:String
     */
    @PostMapping("/driver")
    public Mono<R<Map<Long, String>>> driverStatus(@RequestBody(required = false) DriverQuery driverQuery) {
        try {
            driverQuery.setTenantId(getTenantId());
            Map<Long, String> statuses = driverStatusService.selectByPage(driverQuery);
            return Mono.just(R.ok(statuses));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
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
