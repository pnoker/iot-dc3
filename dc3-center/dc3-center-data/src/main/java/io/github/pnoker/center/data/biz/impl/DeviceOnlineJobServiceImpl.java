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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.api.center.manager.DeviceApiGrpc;
import io.github.pnoker.api.center.manager.GrpcPageDeviceDTO;
import io.github.pnoker.api.center.manager.GrpcPageDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcRPageDeviceDTO;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.center.data.biz.DeviceOnlineJobService;
import io.github.pnoker.center.data.dal.DeviceStatusHistoryManager;
import io.github.pnoker.center.data.entity.model.DeviceStatusHistoryDO;
import io.github.pnoker.center.data.entity.query.DeviceQuery;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.redis.service.RedisService;
import jakarta.annotation.Resource;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DeviceOnlineJobServiceImpl implements DeviceOnlineJobService {
    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub;

    @Resource
    private RedisService redisService;

    @Resource
    private DeviceStatusHistoryManager deviceStatusHistoryService;

    @Override
    public void deviceOnline() {
        DeviceQuery deviceQuery = new DeviceQuery();
        deviceQuery.setTenantId(1L);
        deviceQuery.setPage(new Pages());
        deviceQuery.getPage().setCurrent(1);
        deviceQuery.getPage().setSize(99999);
        GrpcPage.Builder page = GrpcPage.newBuilder()
                .setSize(deviceQuery.getPage().getSize())
                .setCurrent(deviceQuery.getPage().getCurrent());
        GrpcPageDeviceQuery.Builder query = GrpcPageDeviceQuery.newBuilder()
                .setPage(page);
        if (CharSequenceUtil.isNotEmpty(deviceQuery.getDeviceName())) {
            query.setDeviceName(deviceQuery.getDeviceName());
        }
        if (!Objects.isNull(deviceQuery.getDriverId())) {
            query.setDriverId(deviceQuery.getDriverId());
        }
        if (!Objects.isNull(deviceQuery.getTenantId())) {
            query.setTenantId(deviceQuery.getTenantId());
        }
        Optional.ofNullable(deviceQuery.getEnableFlag()).ifPresentOrElse(flag -> query.setEnableFlag(flag.getIndex()), () -> query.setEnableFlag(DefaultConstant.NULL_INT));
        GrpcRPageDeviceDTO list = deviceApiBlockingStub.list(query.build());
        GrpcPageDeviceDTO data = list.getData();
        List<GrpcDeviceDTO> dataList = data.getDataList();
        if (CollUtil.isNotEmpty(dataList)) {
            List<DeviceStatusHistoryDO> deviceStatusHistoryDOList = new ArrayList<>();
            dataList.forEach(driverDO -> {
                String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + driverDO.getBase().getId();
                String status = redisService.getKey(key);
                status = !Objects.isNull(status) ? status : DriverStatusEnum.OFFLINE.getCode();
                DeviceStatusHistoryDO deviceStatusHistoryDO = new DeviceStatusHistoryDO();
                deviceStatusHistoryDO.setDriverId(driverDO.getDriverId());
                deviceStatusHistoryDO.setDeviceId(driverDO.getBase().getId());
                deviceStatusHistoryDO.setDeviceName(driverDO.getDeviceName());
                deviceStatusHistoryDO.setStatus(status);
                deviceStatusHistoryDOList.add(deviceStatusHistoryDO);
            });
            if (!deviceStatusHistoryDOList.isEmpty()) {
                deviceStatusHistoryService.saveBatch(deviceStatusHistoryDOList);
            }
        }
    }
}
