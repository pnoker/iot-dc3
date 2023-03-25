/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.data.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.EnableFlagDTOEnum;
import io.github.pnoker.api.common.MultiFlagDTOEnum;
import io.github.pnoker.api.common.PageDTO;
import io.github.pnoker.center.data.entity.vo.query.DevicePageQuery;
import io.github.pnoker.center.data.service.DeviceStatusService;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.utils.RedisUtil;
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

    @GrpcClient(ManagerServiceConstant.SERVICE_NAME)
    private DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public Map<String, String> device(DevicePageQuery pageQuery) {
        PageDTO.Builder page = PageDTO.newBuilder()
                .setSize(pageQuery.getPage().getSize())
                .setCurrent(pageQuery.getPage().getCurrent());
        DeviceDTO.Builder builder = buildDTOByQuery(pageQuery);
        PageDeviceQueryDTO.Builder query = PageDeviceQueryDTO.newBuilder()
                .setPage(page)
                .setDevice(builder);
        if (CharSequenceUtil.isNotEmpty(pageQuery.getProfileId())) {
            query.setProfileId(pageQuery.getProfileId());
        }
        RPageDeviceDTO rPageDeviceDTO = deviceApiBlockingStub.list(query.build());

        if (!rPageDeviceDTO.getResult().getOk()) {
            return new HashMap<>();
        }

        List<DeviceDTO> devices = rPageDeviceDTO.getData().getDataList();
        return getStatusMap(devices);
    }

    @Override
    public Map<String, String> deviceByProfileId(String profileId) {
        ByProfileQueryDTO query = ByProfileQueryDTO.newBuilder()
                .setProfileId(profileId)
                .build();
        RDeviceListDTO rDeviceListDTO = deviceApiBlockingStub.selectByProfileId(query);
        if (!rDeviceListDTO.getResult().getOk()) {
            return new HashMap<>();
        }

        List<DeviceDTO> devices = rDeviceListDTO.getDataList();
        return getStatusMap(devices);
    }

    private static DeviceDTO.Builder buildDTOByQuery(DevicePageQuery pageQuery) {
        DeviceDTO.Builder dtoBuilder = DeviceDTO.newBuilder();
        if (CharSequenceUtil.isNotEmpty(pageQuery.getDeviceName())) {
            dtoBuilder.setDeviceName(pageQuery.getDeviceName());
        }
        if (CharSequenceUtil.isNotEmpty(pageQuery.getDriverId())) {
            dtoBuilder.setDriverId(pageQuery.getDriverId());
        }
        if (ObjectUtil.isNotNull(pageQuery.getEnableFlag())) {
            dtoBuilder.setMultiFlag(MultiFlagDTOEnum.valueOf(pageQuery.getMultiFlag().name()));
        }
        if (ObjectUtil.isNotNull(pageQuery.getEnableFlag())) {
            dtoBuilder.setEnableFlag(EnableFlagDTOEnum.valueOf(pageQuery.getEnableFlag().name()));
        }
        if (CharSequenceUtil.isNotEmpty(pageQuery.getTenantId())) {
            dtoBuilder.setTenantId(pageQuery.getTenantId());
        }
        return dtoBuilder;
    }

    private Map<String, String> getStatusMap(List<DeviceDTO> devices) {
        Map<String, String> statusMap = new HashMap<>(16);
        Set<String> deviceIds = devices.stream().map(d -> d.getBase().getId()).collect(Collectors.toSet());
        deviceIds.forEach(id -> {
            String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + id;
            String status = redisUtil.getKey(key);
            status = ObjectUtil.isNotNull(status) ? status : DriverStatusEnum.OFFLINE.getCode();
            statusMap.put(id, status);
        });
        return statusMap;
    }

}
