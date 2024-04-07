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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.center.data.biz.DeviceStatisticsOnlineService;
import io.github.pnoker.center.data.dal.DeviceRunHistoryManager;
import io.github.pnoker.center.data.dal.DeviceRunManager;
import io.github.pnoker.center.data.entity.model.DeviceRunDO;
import io.github.pnoker.center.data.entity.model.DeviceRunHistoryDO;
import io.github.pnoker.center.data.entity.model.DeviceStatusHistoryDO;
import io.github.pnoker.center.data.entity.query.DeviceQuery;
import io.github.pnoker.center.data.service.DeviceRunHistoryService;
import io.github.pnoker.center.data.service.DeviceStatusHistoryService;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverStatusEnum;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeviceStatisticsOnlineServiceImpl implements DeviceStatisticsOnlineService {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub;

    @Resource
    private DeviceStatusHistoryService deviceStatusHistoryService;

    @Resource
    private DeviceRunHistoryManager deviceRunHistoryManager;

    @Resource
    private DeviceRunHistoryService deviceRunHistoryService;

    @Resource
    private DeviceRunManager deviceRunManager;

    @Override
    public void deviceStatisticsOnline() {
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
        if (ObjectUtil.isNotEmpty(deviceQuery.getDriverId())) {
            query.setDriverId(deviceQuery.getDriverId());
        } else {
            query.setDriverId(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotNull(deviceQuery.getEnableFlag())) {
            query.setEnableFlag(deviceQuery.getEnableFlag().getIndex());
        } else {
            query.setEnableFlag(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotEmpty(deviceQuery.getTenantId())) {
            query.setTenantId(deviceQuery.getTenantId());
        }
        GrpcRPageDeviceDTO list = deviceApiBlockingStub.list(query.build());
        GrpcPageDeviceDTO data = list.getData();
        List<GrpcDeviceDTO> dataList = data.getDataList();
        if (!dataList.isEmpty()) {
            dataList.forEach(
                    driverDO -> {
                        //查出状态表最近两条数据
                        List<DeviceStatusHistoryDO> deviceStatusHistoryDOS = deviceStatusHistoryService.selectRecently2Data(driverDO.getBase().getId());
                        if (deviceStatusHistoryDOS.size() > 1) {
                            Duration duration = Duration.between(deviceStatusHistoryDOS.get(1).getCreateTime(), deviceStatusHistoryDOS.get(0).getCreateTime());
                            DeviceRunHistoryDO deviceRunHistoryDO = new DeviceRunHistoryDO();
                            long minutes = duration.toMinutes();
                            deviceRunHistoryDO.setDuration(minutes);
                            deviceRunHistoryDO.setDeviceName(driverDO.getDeviceName());
                            deviceRunHistoryDO.setDriverId(driverDO.getDriverId());
                            deviceRunHistoryDO.setDeviceId(driverDO.getBase().getId());
                            if (DriverStatusEnum.OFFLINE.getCode().equals(deviceStatusHistoryDOS.get(0).getStatus())) {
                                if (DriverStatusEnum.OFFLINE.getCode().equals(deviceStatusHistoryDOS.get(1).getStatus())) {
                                    //都为离线  离线时长
                                    deviceRunHistoryDO.setStatus(DriverStatusEnum.OFFLINE.getCode());
                                    deviceRunHistoryManager.save(deviceRunHistoryDO);
                                } else {
                                    // 在线时长
                                    deviceRunHistoryDO.setStatus(DriverStatusEnum.ONLINE.getCode());
                                    deviceRunHistoryManager.save(deviceRunHistoryDO);
                                }
                            } else if (DriverStatusEnum.ONLINE.getCode().equals(deviceStatusHistoryDOS.get(0).getStatus())) {
                                if (DriverStatusEnum.ONLINE.getCode().equals(deviceStatusHistoryDOS.get(1).getStatus())) {
                                    //都为在线  在线时长
                                    deviceRunHistoryDO.setStatus(DriverStatusEnum.ONLINE.getCode());
                                    deviceRunHistoryManager.save(deviceRunHistoryDO);
                                } else {
                                    //离线时长
                                    deviceRunHistoryDO.setStatus(DriverStatusEnum.OFFLINE.getCode());
                                    deviceRunHistoryManager.save(deviceRunHistoryDO);
                                }
                            }
                        }
                    }
            );
        }
        // 查出所有driverids  去重
        List<DeviceRunHistoryDO> deviceRunHistoryDOS = deviceRunHistoryService.list(new LambdaQueryWrapper<>());
        Set<Long> deviceIds = deviceRunHistoryDOS.stream().map(DeviceRunHistoryDO::getDeviceId).collect(Collectors.toSet());
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        deviceIds.forEach(
                id -> {
                    //查出每天统计表在线时长是否有数据
                    DeviceRunDO runDO = deviceRunManager.getOne(new LambdaQueryWrapper<DeviceRunDO>().eq(DeviceRunDO::getDeviceId, id)
                            .eq(DeviceRunDO::getStatus, DriverStatusEnum.ONLINE.getCode())
                            .between(DeviceRunDO::getCreateTime, startOfDay, endOfDay));
                    DeviceRunDO deviceRunDO = deviceRunHistoryService.getDurationDay(id, DriverStatusEnum.ONLINE.getCode(), startOfDay, endOfDay);
                    if (runDO != null && deviceRunDO != null) {
                        deviceRunDO.setId(runDO.getId());
                        deviceRunManager.updateById(deviceRunDO);
                    } else if (ObjectUtils.isEmpty(runDO) && deviceRunDO != null) {
                        deviceRunManager.save(deviceRunDO);
                    }
                    //查出每天统计表离线时长是否有数据
                    DeviceRunDO runOffDO = deviceRunManager.getOne(new LambdaQueryWrapper<DeviceRunDO>().eq(DeviceRunDO::getDeviceId, id)
                            .eq(DeviceRunDO::getStatus, DriverStatusEnum.OFFLINE.getCode())
                            .between(DeviceRunDO::getCreateTime, startOfDay, endOfDay));
                    DeviceRunDO deviceOffRunDO = deviceRunHistoryService.getDurationDay(id, DriverStatusEnum.OFFLINE.getCode(), startOfDay, endOfDay);
                    if (runOffDO != null && deviceOffRunDO != null) {
                        deviceOffRunDO.setId(runOffDO.getId());
                        deviceRunManager.updateById(deviceOffRunDO);
                    } else if (ObjectUtils.isEmpty(runOffDO) && deviceOffRunDO != null) {
                        deviceRunManager.save(deviceOffRunDO);
                    }
                }
        );
    }
}
