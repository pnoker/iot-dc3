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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.pnoker.api.center.manager.DriverApiGrpc;
import io.github.pnoker.api.center.manager.GrpcPageDriverDTO;
import io.github.pnoker.api.center.manager.GrpcPageDriverQuery;
import io.github.pnoker.api.center.manager.GrpcRPageDriverDTO;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.center.data.biz.DriverStatisticsOnlineService;
import io.github.pnoker.center.data.dal.DriverRunHistoryManager;
import io.github.pnoker.center.data.dal.DriverRunManager;
import io.github.pnoker.center.data.entity.model.DriverRunDO;
import io.github.pnoker.center.data.entity.model.DriverRunHistoryDO;
import io.github.pnoker.center.data.entity.model.DriverStatusHistoryDO;
import io.github.pnoker.center.data.entity.query.DriverQuery;
import io.github.pnoker.center.data.service.DriverRunHistoryService;
import io.github.pnoker.center.data.service.DriverStatusHistoryService;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverStatusEnum;
import jakarta.annotation.Resource;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DriverStatisticsOnlineServiceImpl implements DriverStatisticsOnlineService {
    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

    @Resource
    private DriverStatusHistoryService driverStatusHistoryService;

    @Resource
    private DriverRunHistoryManager driverRunHistoryManager;

    @Resource
    private DriverRunHistoryService driverRunHistoryService;

    @Resource
    private DriverRunManager driverRunManager;

    @Override
    public void driverStatisticsOnline() {
        DriverQuery driverQuery = new DriverQuery();
        driverQuery.setTenantId(1L);
        driverQuery.setPage(new Pages());
        driverQuery.getPage().setCurrent(1);
        driverQuery.getPage().setSize(99999);
        GrpcPage.Builder page = GrpcPage.newBuilder()
                .setSize(driverQuery.getPage().getSize())
                .setCurrent(driverQuery.getPage().getCurrent());
        GrpcPageDriverQuery.Builder query = GrpcPageDriverQuery.newBuilder()
                .setPage(page);
        if (CharSequenceUtil.isNotEmpty(driverQuery.getDriverName())) {
            query.setDriverName(driverQuery.getDriverName());
        }
        if (CharSequenceUtil.isNotEmpty(driverQuery.getServiceName())) {
            query.setServiceName(driverQuery.getServiceName());
        }
        if (CharSequenceUtil.isNotEmpty(driverQuery.getServiceHost())) {
            query.setServiceHost(driverQuery.getServiceHost());
        }
        if (Objects.nonNull(driverQuery.getTenantId())) {
            query.setTenantId(driverQuery.getTenantId());
        }
        Optional.ofNullable(driverQuery.getDriverTypeFlag()).ifPresentOrElse(value -> query.setDriverTypeFlag(value.getIndex()), () -> query.setDriverTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(driverQuery.getEnableFlag()).ifPresentOrElse(value -> query.setEnableFlag(value.getIndex()), () -> query.setEnableFlag(DefaultConstant.NULL_INT));
        GrpcRPageDriverDTO list = driverApiBlockingStub.selectByPage(query.build());
        GrpcPageDriverDTO data = list.getData();
        List<GrpcDriverDTO> dataList = data.getDataList();
        if (dataList != null && dataList.size() > 0) {
            dataList.forEach(
                    driverDO -> {
                        //查出状态表最近两条数据
                        List<DriverStatusHistoryDO> driverStatusHistoryDOList = driverStatusHistoryService.selectRecently2Data(driverDO.getBase().getId());
                        if (driverStatusHistoryDOList.size() > 1) {
                            Duration duration = Duration.between(driverStatusHistoryDOList.get(1).getCreateTime(), driverStatusHistoryDOList.get(0).getCreateTime());
                            DriverRunHistoryDO driverRunHistoryDO = new DriverRunHistoryDO();
                            long minutes = duration.toMinutes();
                            driverRunHistoryDO.setDuration(minutes);
                            driverRunHistoryDO.setDriverName(driverDO.getDriverName());
                            driverRunHistoryDO.setDriverId(driverDO.getBase().getId());
                            if (DriverStatusEnum.OFFLINE.getCode().equals(driverStatusHistoryDOList.get(0).getStatus())) {
                                if (DriverStatusEnum.OFFLINE.getCode().equals(driverStatusHistoryDOList.get(1).getStatus())) {
                                    //都为离线  离线时长
                                    driverRunHistoryDO.setStatus(DriverStatusEnum.OFFLINE.getCode());
                                    driverRunHistoryManager.save(driverRunHistoryDO);
                                } else {
                                    // 在线时长
                                    driverRunHistoryDO.setStatus(DriverStatusEnum.ONLINE.getCode());
                                    driverRunHistoryManager.save(driverRunHistoryDO);
                                }
                            } else if (DriverStatusEnum.ONLINE.getCode().equals(driverStatusHistoryDOList.get(0).getStatus())) {
                                if (DriverStatusEnum.ONLINE.getCode().equals(driverStatusHistoryDOList.get(1).getStatus())) {
                                    //都为在线  在线时长
                                    driverRunHistoryDO.setStatus(DriverStatusEnum.ONLINE.getCode());
                                    driverRunHistoryManager.save(driverRunHistoryDO);
                                } else {
                                    //离线时长
                                    driverRunHistoryDO.setStatus(DriverStatusEnum.OFFLINE.getCode());
                                    driverRunHistoryManager.save(driverRunHistoryDO);
                                }
                            }
                        }
                    }
            );
        }
        // 查出所有driverids  去重
        List<DriverRunHistoryDO> driverRunHistoryDOList = driverRunHistoryService.list(new LambdaQueryWrapper<>());
        Set<Long> driverIds = driverRunHistoryDOList.stream().map(e -> e.getDriverId()).collect(Collectors.toSet());
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        if (driverIds != null) {
            driverIds.forEach(
                    id -> {
                        //查出每天统计表在线时长是否有数据
                        DriverRunDO runDO = driverRunManager.getOne(new LambdaQueryWrapper<DriverRunDO>().eq(DriverRunDO::getDriverId, id)
                                .eq(DriverRunDO::getStatus, DriverStatusEnum.ONLINE.getCode())
                                .between(DriverRunDO::getCreateTime, startOfDay, endOfDay));
                        DriverRunDO driverRunDO = driverRunHistoryService.getDurationDay(id, DriverStatusEnum.ONLINE.getCode(), startOfDay, endOfDay);
                        if (runDO != null && driverRunDO != null) {
                            driverRunDO.setId(runDO.getId());
                            driverRunManager.updateById(driverRunDO);
                        } else if (Objects.isNull(runDO) && driverRunDO != null) {
                            driverRunManager.save(driverRunDO);
                        }
                        //查出每天统计表离线时长是否有数据
                        DriverRunDO runOffDO = driverRunManager.getOne(new LambdaQueryWrapper<DriverRunDO>().eq(DriverRunDO::getDriverId, id)
                                .eq(DriverRunDO::getStatus, DriverStatusEnum.OFFLINE.getCode())
                                .between(DriverRunDO::getCreateTime, startOfDay, endOfDay));
                        DriverRunDO driverOffRunDO = driverRunHistoryService.getDurationDay(id, DriverStatusEnum.OFFLINE.getCode(), startOfDay, endOfDay);
                        if (runOffDO != null && driverOffRunDO != null) {
                            driverOffRunDO.setId(runOffDO.getId());
                            driverRunManager.updateById(driverOffRunDO);
                        } else if (Objects.isNull(runOffDO) && driverOffRunDO != null) {
                            driverRunManager.save(driverOffRunDO);
                        }
                    }
            );
        }

    }
}
