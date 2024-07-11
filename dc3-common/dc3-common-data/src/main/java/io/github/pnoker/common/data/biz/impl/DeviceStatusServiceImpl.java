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

package io.github.pnoker.common.data.biz.impl;

import cn.hutool.core.map.MapUtil;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.data.biz.DeviceStatusService;
import io.github.pnoker.common.data.entity.bo.DeviceRunBO;
import io.github.pnoker.common.data.entity.model.DeviceRunDO;
import io.github.pnoker.common.data.entity.query.DeviceQuery;
import io.github.pnoker.common.data.service.DeviceRunService;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.redis.service.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

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
    public Map<Long, String> selectByPage(DeviceQuery pageQuery) {
        GrpcPage.Builder page = GrpcPage.newBuilder().setSize(pageQuery.getPage().getSize()).setCurrent(pageQuery.getPage().getCurrent());
        GrpcPageDeviceQuery.Builder query = GrpcPageDeviceQuery.newBuilder().setPage(page);
        StringOptional.ofNullable(pageQuery.getDeviceName()).ifPresent(query::setDeviceName);
        StringOptional.ofNullable(pageQuery.getDeviceCode()).ifPresent(query::setDeviceCode);
        LongOptional.ofNullable(pageQuery.getDriverId()).ifPresent(query::setDriverId);
        LongOptional.ofNullable(pageQuery.getProfileId()).ifPresent(query::setProfileId);
        LongOptional.ofNullable(pageQuery.getTenantId()).ifPresent(query::setTenantId);
        Optional.ofNullable(pageQuery.getEnableFlag()).ifPresentOrElse(value -> query.setEnableFlag(value.getIndex()), () -> query.setEnableFlag(DefaultConstant.NULL_INT));
        GrpcRPageDeviceDTO rPageDeviceDTO = deviceApiBlockingStub.selectByPage(query.build());

        if (!rPageDeviceDTO.getResult().getOk()) {
            return MapUtil.empty();
        }

        List<GrpcDeviceDTO> devices = rPageDeviceDTO.getData().getDataList();
        return getStatusMap(devices);
    }

    @Override
    public Map<Long, String> selectByProfileId(Long profileId) {
        GrpcProfileQuery query = GrpcProfileQuery.newBuilder().setProfileId(profileId).build();
        GrpcRDeviceListDTO rDeviceListDTO = deviceApiBlockingStub.selectByProfileId(query);
        if (!rDeviceListDTO.getResult().getOk()) {
            return MapUtil.empty();
        }

        List<GrpcDeviceDTO> devices = rDeviceListDTO.getDataList();
        return getStatusMap(devices);
    }

    @Override
    public DeviceRunBO selectOnlineByDeviceId(Long deviceId) {
        List<DeviceRunDO> deviceRunDOList = deviceRunService.get7daysDuration(deviceId, DriverStatusEnum.ONLINE.getCode());
        Long totalDuration = deviceRunService.selectSumDuration(deviceId, DriverStatusEnum.ONLINE.getCode());
        GrpcDeviceQuery.Builder builder = GrpcDeviceQuery.newBuilder();
        builder.setDeviceId(deviceId);
        GrpcRDeviceDTO rDeviceDTO = deviceApiBlockingStub.selectByDeviceId(builder.build());
        if (!rDeviceDTO.getResult().getOk()) {
            throw new RuntimeException("Device does not exist");
        }
        DeviceRunBO deviceRunBO = new DeviceRunBO();
        List<Long> zeroList = Collections.nCopies(7, 0L);
        ArrayList<Long> list = new ArrayList<>(zeroList);
        deviceRunBO.setStatus(DriverStatusEnum.ONLINE.getCode());
        deviceRunBO.setTotalDuration(totalDuration == null ? 0L : totalDuration);
        deviceRunBO.setDeviceName(rDeviceDTO.getData().getDeviceName());
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
        GrpcDeviceQuery.Builder builder = GrpcDeviceQuery.newBuilder();
        builder.setDeviceId(deviceId);
        GrpcRDeviceDTO rDeviceDTO = deviceApiBlockingStub.selectByDeviceId(builder.build());
        if (!rDeviceDTO.getResult().getOk()) {
            throw new RuntimeException("Device does not exist");
        }
        DeviceRunBO deviceRunBO = new DeviceRunBO();
        List<Long> zeroList = Collections.nCopies(7, 0L);
        ArrayList<Long> list = new ArrayList<>(zeroList);
        deviceRunBO.setStatus(DriverStatusEnum.OFFLINE.getCode());
        deviceRunBO.setTotalDuration(totalDuration == null ? 0L : totalDuration);
        deviceRunBO.setDeviceName(rDeviceDTO.getData().getDeviceName());
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
     * 获取设备状态 Map
     *
     * @param devices GrpcDeviceDTO Array
     * @return 状态 Map
     */
    private Map<Long, String> getStatusMap(List<GrpcDeviceDTO> devices) {
        Map<Long, String> statusMap = new HashMap<>(16);
        Set<Long> deviceIds = devices.stream().map(d -> d.getBase().getId()).collect(Collectors.toSet());
        deviceIds.forEach(id -> {
            String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + id;
            String status = redisService.getKey(key);
            status = Objects.nonNull(status) ? status : DeviceStatusEnum.OFFLINE.getCode();
            statusMap.put(id, status);
        });
        return statusMap;
    }

}
