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
import io.github.pnoker.center.data.biz.DeviceOnlineJobService;
import io.github.pnoker.center.data.dal.DeviceStatusHistoryManager;
import io.github.pnoker.center.data.entity.model.DeviceStatusHistoryDO;
import io.github.pnoker.center.data.entity.query.DeviceQuery;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.redis.RedisService;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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
        GrpcPageDTO.Builder page = GrpcPageDTO.newBuilder()
                .setSize(deviceQuery.getPage().getSize())
                .setCurrent(deviceQuery.getPage().getCurrent());
        DeviceDTO.Builder builder = buildDTOByQuery(deviceQuery);
        GrpcPageDeviceQueryDTO.Builder query = GrpcPageDeviceQueryDTO.newBuilder()
                .setPage(page)
                .setDevice(builder);
        GrpcRPageDeviceDTO list = deviceApiBlockingStub.list(query.build());
        GrpcPageDeviceDTO data = list.getData();
        List<DeviceDTO> dataList = data.getDataList();
        if (ObjectUtils.isNotEmpty(dataList)) {
            List<DeviceStatusHistoryDO> deviceStatusHistoryDOS = new ArrayList<>();
            dataList.forEach(driverDO -> {
                String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + driverDO.getBase().getId();
                String status = redisService.getKey(key);
                status = ObjectUtil.isNotNull(status) ? status : DriverStatusEnum.OFFLINE.getCode();
                DeviceStatusHistoryDO deviceStatusHistoryDO = new DeviceStatusHistoryDO();
                deviceStatusHistoryDO.setDriverId(driverDO.getDriverId());
                deviceStatusHistoryDO.setDeviceId(driverDO.getBase().getId());
                deviceStatusHistoryDO.setDeviceName(driverDO.getDeviceName());
                deviceStatusHistoryDO.setStatus(status);
                deviceStatusHistoryDOS.add(deviceStatusHistoryDO);
            });
            if (deviceStatusHistoryDOS != null && deviceStatusHistoryDOS.size() > 0) {
                deviceStatusHistoryService.saveBatch(deviceStatusHistoryDOS);
            }
        }
    }

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
}
