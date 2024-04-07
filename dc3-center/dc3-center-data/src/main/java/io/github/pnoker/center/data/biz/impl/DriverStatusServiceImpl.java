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
import io.github.pnoker.center.data.biz.DriverStatusService;
import io.github.pnoker.center.data.entity.bo.DriverRunBO;
import io.github.pnoker.center.data.entity.builder.DriverDurationBuilder;
import io.github.pnoker.center.data.entity.model.DriverRunDO;
import io.github.pnoker.center.data.entity.query.DriverQuery;
import io.github.pnoker.center.data.service.DriverRunService;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DriverService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverStatusServiceImpl implements DriverStatusService {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

    @Resource
    private RedisService redisService;
    @Resource
    private DriverRunService driverRunService;

    @Resource
    private DriverDurationBuilder driverDurationBuilder;

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub;

    @Override
    public Map<Long, String> driver(DriverQuery pageQuery) {
        GrpcPage.Builder page = GrpcPage.newBuilder()
                .setSize(pageQuery.getPage().getSize())
                .setCurrent(pageQuery.getPage().getCurrent());
        GrpcPageDriverQuery.Builder query = GrpcPageDriverQuery.newBuilder()
                .setPage(page);
        if (CharSequenceUtil.isNotEmpty(pageQuery.getDriverName())) {
            query.setDriverName(pageQuery.getDriverName());
        }
        if (CharSequenceUtil.isNotEmpty(pageQuery.getServiceName())) {
            query.setServiceName(pageQuery.getServiceName());
        }
        if (CharSequenceUtil.isNotEmpty(pageQuery.getServiceHost())) {
            query.setServiceHost(pageQuery.getServiceHost());
        }
        if (ObjectUtil.isNotNull(pageQuery.getDriverTypeFlag())) {
            query.setDriverTypeFlag(pageQuery.getDriverTypeFlag().getIndex());
        } else {
            query.setDriverTypeFlag(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotNull(pageQuery.getEnableFlag())) {
            query.setEnableFlag(pageQuery.getEnableFlag().getIndex());
        } else {
            query.setEnableFlag(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotEmpty(pageQuery.getTenantId())) {
            query.setTenantId(pageQuery.getTenantId());
        }
        GrpcRPageDriverDTO rPageDriverDTO = driverApiBlockingStub.list(query.build());

        if (!rPageDriverDTO.getResult().getOk()) {
            return new HashMap<>();
        }

        List<GrpcDriverDTO> drivers = rPageDriverDTO.getData().getDataList();
        return getStatusMap(drivers);
    }

    @Override
    public DriverRunBO selectOnlineByDriverId(Long driverId) {
        List<DriverRunDO> driverRunDOS = driverRunService.get7daysDuration(driverId, DriverStatusEnum.ONLINE.getCode());
        GrpcDriverQuery.Builder builder = GrpcDriverQuery.newBuilder();
        builder.setDriverId(driverId);
        GrpcRDriverDTO rDriverDTO = driverApiBlockingStub.selectByDriverId(builder.build());
        if (!rDriverDTO.getResult().getOk()) {
            throw new RuntimeException("Grpc Failed");
        }
        DriverRunBO driverRunBO = new DriverRunBO();
        List<Long> zeroList = Collections.nCopies(7, 0L);
        ArrayList<Long> list = new ArrayList<>(zeroList);
        driverRunBO.setDriverName(rDriverDTO.getData().getDriverName());
        driverRunBO.setStatus(DriverStatusEnum.ONLINE.getCode());
        if (ObjectUtil.isEmpty(driverRunDOS)) {
            driverRunBO.setDuration(list);
            return driverRunBO;
        }
        for (int i = 0; i < driverRunDOS.size(); i++) {
            list.set(i, driverRunDOS.get(i).getDuration());
        }
        driverRunBO.setDuration(list);
        return driverRunBO;
    }

    @Override
    public DriverRunBO selectOfflineByDriverId(Long driverId) {
        List<DriverRunDO> driverRunDOS = driverRunService.get7daysDuration(driverId, DriverStatusEnum.OFFLINE.getCode());
        GrpcDriverQuery.Builder builder = GrpcDriverQuery.newBuilder();
        builder.setDriverId(driverId);
        GrpcRDriverDTO rDriverDTO = driverApiBlockingStub.selectByDriverId(builder.build());
        if (!rDriverDTO.getResult().getOk()) {
            throw new RuntimeException("Grpc Failed");
        }
        DriverRunBO driverRunBO = new DriverRunBO();
        List<Long> zeroList = Collections.nCopies(7, 0L);
        ArrayList<Long> list = new ArrayList<>(zeroList);
        driverRunBO.setStatus(DriverStatusEnum.OFFLINE.getCode());
        driverRunBO.setDriverName(rDriverDTO.getData().getDriverName());
        if (ObjectUtil.isEmpty(driverRunDOS)) {
            driverRunBO.setDuration(list);
            return driverRunBO;
        }
        for (int i = 0; i < driverRunDOS.size(); i++) {
            list.set(i, driverRunDOS.get(i).getDuration());
        }
        driverRunBO.setDuration(list);
        return driverRunBO;
    }

    @Override
    public String getDeviceOnlineByDriverId(Long driverId) {
        List<String> list = getList(driverId);
        if (list == null) return null;
        long count = list.stream().filter(e -> e.equals(DeviceStatusEnum.ONLINE.getCode())).count();
        return String.valueOf(count);
    }

    @Override
    public String getDeviceOfflineByDriverId(Long driverId) {
        List<String> list = getList(driverId);
        if (list == null) return null;
        long count = list.stream().filter(e -> e.equals(DeviceStatusEnum.OFFLINE.getCode())).count();
        return String.valueOf(count);
    }

    /**
     * get deviceList  Online/Offline BY driverId
     *
     * @param driverId
     * @return
     */
    @Nullable
    private List<String> getList(Long driverId) {
        GrpcDriverQuery query = GrpcDriverQuery.newBuilder()
                .setDriverId(driverId)
                .build();
        GrpcRDeviceListDTO onlineByDriverId = deviceApiBlockingStub.selectByDriverId(query);
        if (!onlineByDriverId.getResult().getOk()) {
            return null;
        }
        List<GrpcDeviceDTO> devices = onlineByDriverId.getDataList();
        Set<Long> deviceIds = devices.stream().map(d -> d.getBase().getId()).collect(Collectors.toSet());
        List<String> list = new ArrayList<>();
        deviceIds.forEach(id -> {
            String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + id;
            String status = redisService.getKey(key);
            status = ObjectUtil.isNotNull(status) ? status : DeviceStatusEnum.OFFLINE.getCode();
            list.add(status);
        });
        return list;
    }

    /**
     * Get status map
     *
     * @param drivers DriverDTO Array
     * @return Status Map
     */
    private Map<Long, String> getStatusMap(List<GrpcDriverDTO> drivers) {
        Map<Long, String> statusMap = new HashMap<>(16);
        Set<Long> driverIds = drivers.stream().map(d -> d.getBase().getId()).collect(Collectors.toSet());
        driverIds.forEach(id -> {
            String key = PrefixConstant.DRIVER_STATUS_KEY_PREFIX + id;
            String status = redisService.getKey(key);
            status = ObjectUtil.isNotNull(status) ? status : DriverStatusEnum.OFFLINE.getCode();
            statusMap.put(id, status);
        });
        return statusMap;
    }

}
