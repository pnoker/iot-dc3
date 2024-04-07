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

package io.github.pnoker.center.data.biz.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.center.data.biz.DeviceStatusService;
import io.github.pnoker.center.data.entity.bo.DeviceRunBO;
import io.github.pnoker.center.data.entity.model.DeviceRunDO;
import io.github.pnoker.center.data.entity.query.DeviceQuery;
import io.github.pnoker.center.data.service.DeviceRunService;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DeviceService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DeviceStatusServiceImpl implements DeviceStatusService {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub;

    @Resource
    private RedisService redisService;

    @Resource
    private DeviceRunService deviceRunService;

    @Override
    public Map<Long, String> device(DeviceQuery pageQuery) {
        GrpcPage.Builder page = GrpcPage.newBuilder()
                .setSize(pageQuery.getPage().getSize())
                .setCurrent(pageQuery.getPage().getCurrent());
        GrpcPageDeviceQuery.Builder query = GrpcPageDeviceQuery.newBuilder()
                .setPage(page);
        if (CharSequenceUtil.isNotEmpty(pageQuery.getDeviceName())) {
            query.setDeviceName(pageQuery.getDeviceName());
        }
        if (ObjectUtil.isNotEmpty(pageQuery.getDriverId())) {
            query.setDriverId(pageQuery.getDriverId());
        } else {
            query.setDriverId(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotNull(pageQuery.getEnableFlag())) {
            query.setEnableFlag(pageQuery.getEnableFlag().getIndex());
        } else {
            query.setEnableFlag(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotEmpty(pageQuery.getTenantId())) {
            query.setTenantId(pageQuery.getTenantId());
        }
        if (ObjectUtil.isNotEmpty(pageQuery.getProfileId())) {
            query.setProfileId(pageQuery.getProfileId());
        }
        GrpcRPageDeviceDTO rPageDeviceDTO = deviceApiBlockingStub.list(query.build());

        if (!rPageDeviceDTO.getResult().getOk()) {
            return new HashMap<>();
        }

        List<GrpcDeviceDTO> devices = rPageDeviceDTO.getData().getDataList();
        return getStatusMap(devices);
    }

    @Override
    public Map<Long, String> deviceByProfileId(Long profileId) {
        GrpcProfileQuery query = GrpcProfileQuery.newBuilder()
                .setProfileId(profileId)
                .build();
        GrpcRDeviceListDTO rDeviceListDTO = deviceApiBlockingStub.selectByProfileId(query);
        if (!rDeviceListDTO.getResult().getOk()) {
            return new HashMap<>();
        }

        List<GrpcDeviceDTO> devices = rDeviceListDTO.getDataList();
        return getStatusMap(devices);
    }

    @Override
    public DeviceRunBO selectOnlineByDeviceId(Long deviceId) {
        List<DeviceRunDO> deviceRunDOS = deviceRunService.get7daysDuration(deviceId, DriverStatusEnum.ONLINE.getCode());
        GrpcDeviceQuery.Builder builder = GrpcDeviceQuery.newBuilder();
        builder.setDeviceId(deviceId);
        GrpcRDeviceDTO rDeviceDTO = deviceApiBlockingStub.selectByDeviceId(builder.build());
        if (!rDeviceDTO.getResult().getOk()) {
            throw new RuntimeException("Grpc Failed");
        }
        DeviceRunBO deviceRunBO = new DeviceRunBO();
        List<Long> zeroList = Collections.nCopies(7, 0L);
        ArrayList<Long> list = new ArrayList<>(zeroList);
        deviceRunBO.setStatus(DriverStatusEnum.ONLINE.getCode());
        deviceRunBO.setDeviceName(rDeviceDTO.getData().getDeviceName());
        if (ObjectUtil.isEmpty(deviceRunDOS)) {
            deviceRunBO.setDuration(list);
            return deviceRunBO;
        }
        for (int i = 0; i < deviceRunDOS.size(); i++) {
            list.set(i, deviceRunDOS.get(i).getDuration());
        }
        deviceRunBO.setDuration(list);
        return deviceRunBO;
    }

    @Override
    public DeviceRunBO selectOfflineByDeviceId(Long deviceId) {
        List<DeviceRunDO> deviceRunDOS = deviceRunService.get7daysDuration(deviceId, DriverStatusEnum.OFFLINE.getCode());
        GrpcDeviceQuery.Builder builder = GrpcDeviceQuery.newBuilder();
        builder.setDeviceId(deviceId);
        GrpcRDeviceDTO rDeviceDTO = deviceApiBlockingStub.selectByDeviceId(builder.build());
        if (!rDeviceDTO.getResult().getOk()) {
            throw new RuntimeException("Grpc Failed");
        }
        DeviceRunBO deviceRunBO = new DeviceRunBO();
        List<Long> zeroList = Collections.nCopies(7, 0L);
        ArrayList<Long> list = new ArrayList<>(zeroList);
        deviceRunBO.setStatus(DriverStatusEnum.OFFLINE.getCode());
        deviceRunBO.setDeviceName(rDeviceDTO.getData().getDeviceName());
        if (ObjectUtil.isEmpty(deviceRunDOS)) {
            deviceRunBO.setDuration(list);
            return deviceRunBO;
        }
        for (int i = 0; i < deviceRunDOS.size(); i++) {
            list.set(i, deviceRunDOS.get(i).getDuration());
        }
        deviceRunBO.setDuration(list);
        return deviceRunBO;
    }

    /**
     * Get status map
     *
     * @param devices DeviceDTO Array
     * @return Status Map
     */
    private Map<Long, String> getStatusMap(List<GrpcDeviceDTO> devices) {
        Map<Long, String> statusMap = new HashMap<>(16);
        Set<Long> deviceIds = devices.stream().map(d -> d.getBase().getId()).collect(Collectors.toSet());
        deviceIds.forEach(id -> {
            String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + id;
            String status = redisService.getKey(key);
            status = ObjectUtil.isNotNull(status) ? status : DeviceStatusEnum.OFFLINE.getCode();
            statusMap.put(id, status);
        });
        return statusMap;
    }

}
