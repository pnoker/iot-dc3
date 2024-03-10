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
import io.github.pnoker.api.common.GrpcPageDTO;
import io.github.pnoker.center.data.biz.DeviceStatusService;
import io.github.pnoker.center.data.entity.query.DeviceQuery;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Override
    public Map<Long, String> device(DeviceQuery pageQuery) {
        GrpcPageDTO.Builder page = GrpcPageDTO.newBuilder()
                .setSize(pageQuery.getPage().getSize())
                .setCurrent(pageQuery.getPage().getCurrent());
        DeviceDTO.Builder builder = buildDTOByQuery(pageQuery);
        GrpcPageDeviceQueryDTO.Builder query = GrpcPageDeviceQueryDTO.newBuilder()
                .setPage(page)
                .setDevice(builder);
        if (ObjectUtil.isNotEmpty(pageQuery.getProfileId())) {
            query.setProfileId(pageQuery.getProfileId());
        }
        GrpcRPageDeviceDTO rPageDeviceDTO = deviceApiBlockingStub.list(query.build());

        if (!rPageDeviceDTO.getResult().getOk()) {
            return new HashMap<>();
        }

        List<DeviceDTO> devices = rPageDeviceDTO.getData().getDataList();
        return getStatusMap(devices);
    }

    @Override
    public Map<Long, String> deviceByProfileId(Long profileId) {
        GrpcByProfileQueryDTO query = GrpcByProfileQueryDTO.newBuilder()
                .setProfileId(profileId)
                .build();
        GrpcRDeviceListDTO rDeviceListDTO = deviceApiBlockingStub.selectByProfileId(query);
        if (!rDeviceListDTO.getResult().getOk()) {
            return new HashMap<>();
        }

        List<DeviceDTO> devices = rDeviceListDTO.getDataList();
        return getStatusMap(devices);
    }

    /**
     * Query to DTO
     *
     * @param pageQuery DevicePageQuery
     * @return DeviceDTO Builder
     */
    private static DeviceDTO.Builder buildDTOByQuery(DeviceQuery pageQuery) {
        DeviceDTO.Builder builder = DeviceDTO.newBuilder();
        if (CharSequenceUtil.isNotEmpty(pageQuery.getDeviceName())) {
            builder.setDeviceName(pageQuery.getDeviceName());
        }
        if (ObjectUtil.isNotEmpty(pageQuery.getDriverId())) {
            builder.setDriverId(pageQuery.getDriverId());
        } else {
            builder.setDriverId(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotNull(pageQuery.getEnableFlag())) {
            builder.setEnableFlag(pageQuery.getEnableFlag().getIndex());
        } else {
            builder.setEnableFlag(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotEmpty(pageQuery.getTenantId())) {
            builder.setTenantId(pageQuery.getTenantId());
        }
        return builder;
    }

    /**
     * Get status map
     *
     * @param devices DeviceDTO Array
     * @return Status Map
     */
    private Map<Long, String> getStatusMap(List<DeviceDTO> devices) {
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
