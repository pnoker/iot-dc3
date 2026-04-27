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

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.data.biz.DeviceStatusService;
import io.github.pnoker.common.data.entity.bo.DeviceRunBO;
import io.github.pnoker.common.data.entity.model.DeviceRunDO;
import io.github.pnoker.common.data.entity.query.DeviceQuery;
import io.github.pnoker.common.data.service.DeviceRunService;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.redis.service.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DeviceService Impl
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DeviceStatusServiceImpl implements DeviceStatusService {

    @Resource
    private DeviceFacade deviceFacade;

    @Resource
    private RedisService redisService;

    @Resource
    private DeviceRunService deviceRunService;

    @Override
    public Map<Long, String> selectByPage(DeviceQuery pageQuery) {
        FacadeDeviceQuery facadeQuery = FacadeDeviceQuery.builder()
                .page(pageQuery.getPage())
                .deviceName(pageQuery.getDeviceName())
                .deviceCode(pageQuery.getDeviceCode())
                .driverId(pageQuery.getDriverId())
                .profileId(pageQuery.getProfileId())
                .tenantId(pageQuery.getTenantId())
                .enableFlag(pageQuery.getEnableFlag())
                .build();

        FacadePage<FacadeDeviceBO> page = deviceFacade.selectByPage(facadeQuery);
        if (page.getRecords().isEmpty()) {
            return Map.of();
        }

        return getStatusMap(page.getRecords());
    }

    @Override
    public Map<Long, String> selectByProfileId(Long profileId) {
        List<FacadeDeviceBO> devices = deviceFacade.selectByProfileId(profileId);
        if (devices.isEmpty()) {
            return Map.of();
        }
        return getStatusMap(devices);
    }

    @Override
    public DeviceRunBO selectOnlineByDeviceId(Long deviceId) {
        List<DeviceRunDO> deviceRunDOList = deviceRunService.get7daysDuration(deviceId, DriverStatusEnum.ONLINE.getCode());
        Long totalDuration = deviceRunService.selectSumDuration(deviceId, DriverStatusEnum.ONLINE.getCode());

        FacadeDeviceBO device = deviceFacade.selectById(deviceId);
        if (Objects.isNull(device)) {
            throw new RuntimeException("Device does not exist");
        }

        DeviceRunBO deviceRunBO = new DeviceRunBO();
        ArrayList<Long> list = new ArrayList<>(Collections.nCopies(7, 0L));
        deviceRunBO.setStatus(DriverStatusEnum.ONLINE.getCode());
        deviceRunBO.setTotalDuration(totalDuration == null ? 0L : totalDuration);
        deviceRunBO.setDeviceName(device.getDeviceName());
        if (Objects.isNull(deviceRunDOList)) {
            deviceRunBO.setDuration(list);
            return deviceRunBO;
        }
        for (int i = 0; i < deviceRunDOList.size(); i++) {
            list.set(i, deviceRunDOList.get(i).getDuration());
        }
        deviceRunBO.setDuration(list);
        return deviceRunBO;
    }

    @Override
    public DeviceRunBO selectOfflineByDeviceId(Long deviceId) {
        List<DeviceRunDO> deviceRunDOList = deviceRunService.get7daysDuration(deviceId, DriverStatusEnum.OFFLINE.getCode());
        Long totalDuration = deviceRunService.selectSumDuration(deviceId, DriverStatusEnum.OFFLINE.getCode());

        FacadeDeviceBO device = deviceFacade.selectById(deviceId);
        if (Objects.isNull(device)) {
            throw new RuntimeException("Device does not exist");
        }

        DeviceRunBO deviceRunBO = new DeviceRunBO();
        ArrayList<Long> list = new ArrayList<>(Collections.nCopies(7, 0L));
        deviceRunBO.setStatus(DriverStatusEnum.OFFLINE.getCode());
        deviceRunBO.setTotalDuration(totalDuration == null ? 0L : totalDuration);
        deviceRunBO.setDeviceName(device.getDeviceName());
        if (Objects.isNull(deviceRunDOList)) {
            deviceRunBO.setDuration(list);
            return deviceRunBO;
        }
        for (int i = 0; i < deviceRunDOList.size(); i++) {
            list.set(i, deviceRunDOList.get(i).getDuration());
        }
        deviceRunBO.setDuration(list);
        return deviceRunBO;
    }

    /**
     * Get a map of device statuses keyed by device id.
     */
    private Map<Long, String> getStatusMap(List<FacadeDeviceBO> devices) {
        Map<Long, String> statusMap = new HashMap<>(16);
        Set<Long> deviceIds = devices.stream().map(FacadeDeviceBO::getId).collect(Collectors.toSet());
        deviceIds.forEach(id -> {
            String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + id;
            String status = redisService.getKey(key);
            status = Objects.nonNull(status) ? status : DeviceStatusEnum.OFFLINE.getCode();
            statusMap.put(id, status);
        });
        return statusMap;
    }
}
