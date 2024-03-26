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

package io.github.pnoker.center.data.controller;

import io.github.pnoker.center.data.biz.DeviceStatusService;
import io.github.pnoker.center.data.entity.bo.DeviceRunBO;
import io.github.pnoker.center.data.entity.builder.DeviceDurationBuilder;
import io.github.pnoker.center.data.entity.query.DeviceQuery;
import io.github.pnoker.center.data.entity.vo.DeviceRunVO;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.R;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 设备 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@Tag(name = "接口-设备状态")
@RequestMapping(DataConstant.DEVICE_STATUS_URL_PREFIX)
public class DeviceStatusController implements BaseController {

    @Resource
    private DeviceStatusService deviceStatusService;

    @Resource
    private DeviceDurationBuilder deviceDurationBuilder;

    /**
     * 查询 Device 服务状态
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param deviceQuery Device Dto
     * @return Map String:String
     */
    @PostMapping("/device")
    public R<Map<Long, String>> deviceStatus(@RequestBody(required = false) DeviceQuery deviceQuery) {
        try {
            deviceQuery.setTenantId(getTenantId());
            Map<Long, String> statuses = deviceStatusService.device(deviceQuery);
            return R.ok(statuses);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 驱动ID 查询 Device 服务状态
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param driverId 驱动ID
     * @return Map String:String
     */
    @GetMapping("/device/driver_id/{driverId}")
    public R<Map<Long, String>> deviceStatusByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            DeviceQuery deviceQuery = new DeviceQuery();
            deviceQuery.setDriverId(driverId);
            Map<Long, String> statuses = deviceStatusService.device(deviceQuery);
            return R.ok(statuses);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 模板ID 查询 Device 服务状态
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param profileId 位号ID
     * @return Map String:String
     */
    @GetMapping("/device/profile_id/{profileId}")
    public R<Map<Long, String>> deviceStatusByProfileId(@NotNull @PathVariable(value = "profileId") Long profileId) {
        try {
            Map<Long, String> statuses = deviceStatusService.deviceByProfileId(profileId);
            return R.ok(statuses);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 查询 device 在线总时长
     *
     * @param deviceId
     * @return
     */
    @GetMapping("/deviceOnline/{deviceId}")
    public R<DeviceRunVO> selectOnlineByDriverId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            DeviceRunBO duration = deviceStatusService.selectOnlineByDeviceId(deviceId);
            DeviceRunVO result = deviceDurationBuilder.buildVOByBOList(duration);
            return R.ok(result);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 查询 device 离线总时长
     *
     * @param deviceId
     * @return
     */
    @GetMapping("/deviceOffline/{deviceId}")
    public R<DeviceRunVO> selectOfflineByDriverId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            DeviceRunBO duration = deviceStatusService.selectOfflineByDeviceId(deviceId);
            DeviceRunVO result = deviceDurationBuilder.buildVOByBOList(duration);
            return R.ok(result);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

}
