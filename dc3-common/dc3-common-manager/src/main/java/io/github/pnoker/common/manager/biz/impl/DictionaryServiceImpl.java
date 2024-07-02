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

package io.github.pnoker.common.manager.biz.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.entity.bo.DictionaryBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.manager.biz.DictionaryService;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.DictionaryForManagerBuilder;
import io.github.pnoker.common.manager.entity.query.*;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.manager.service.ProfileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DictionaryServiceImpl implements DictionaryService {

    @Resource
    private DictionaryForManagerBuilder dictionaryBuilder;

    @Resource
    private DriverService driverService;
    @Resource
    private ProfileService profileService;
    @Resource
    private DeviceService deviceService;
    @Resource
    private PointService pointService;

    @Override
    public Page<DictionaryBO> driverDictionary(DictionaryQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }

        DriverQuery driverQuery = DriverQuery.builder().page(entityQuery.getPage()).driverName(entityQuery.getLabel()).tenantId(entityQuery.getTenantId()).build();
        Page<DriverBO> driverPageBO = driverService.selectByPage(driverQuery);
        return dictionaryBuilder.buildVOPageByDriverBOPage(driverPageBO);
    }

    @Override
    public Page<DictionaryBO> profileDictionary(DictionaryQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }

        ProfileQuery profileQuery = ProfileQuery.builder().page(entityQuery.getPage()).profileName(entityQuery.getLabel()).tenantId(entityQuery.getTenantId()).build();
        Page<ProfileBO> profilePageBO = profileService.selectByPage(profileQuery);
        return dictionaryBuilder.buildVOPageByProfileBOPage(profilePageBO);
    }

    @Override
    public Page<DictionaryBO> pointDictionaryForProfile(DictionaryQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }

        PointQuery pointQuery = PointQuery.builder().page(entityQuery.getPage()).profileId(entityQuery.getParentId()).pointName(entityQuery.getLabel()).tenantId(entityQuery.getTenantId()).build();
        Page<PointBO> pointPageBO = pointService.selectByPage(pointQuery);
        return dictionaryBuilder.buildVOPageByPointBOPage(pointPageBO);
    }

    @Override
    public Page<DictionaryBO> pointDictionaryForDevice(DictionaryQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }

        PointQuery pointQuery = PointQuery.builder().page(entityQuery.getPage()).deviceId(entityQuery.getParentId()).pointName(entityQuery.getLabel()).tenantId(entityQuery.getTenantId()).build();
        Page<PointBO> pointPageBO = pointService.selectByPage(pointQuery);
        return dictionaryBuilder.buildVOPageByPointBOPage(pointPageBO);
    }

    @Override
    public Page<DictionaryBO> deviceDictionary(DictionaryQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }

        DeviceQuery deviceQuery = DeviceQuery.builder().page(entityQuery.getPage()).deviceName(entityQuery.getLabel()).tenantId(entityQuery.getTenantId()).build();
        Page<DeviceBO> devicePageBO = deviceService.selectByPage(deviceQuery);
        return dictionaryBuilder.buildVOPageByDeviceBOPage(devicePageBO);
    }

    @Override
    public Page<DictionaryBO> deviceDictionaryForDriver(DictionaryQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }

        DeviceQuery deviceQuery = DeviceQuery.builder().page(entityQuery.getPage()).driverId(entityQuery.getParentId()).deviceName(entityQuery.getLabel()).tenantId(entityQuery.getTenantId()).build();
        Page<DeviceBO> devicePageBO = deviceService.selectByPage(deviceQuery);
        return dictionaryBuilder.buildVOPageByDeviceBOPage(devicePageBO);
    }

}
