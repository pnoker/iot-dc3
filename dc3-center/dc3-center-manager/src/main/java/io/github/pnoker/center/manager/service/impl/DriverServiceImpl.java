/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.dto.DriverDto;
import io.github.pnoker.center.manager.mapper.DriverMapper;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.common.bean.common.Pages;
import io.github.pnoker.common.entity.Device;
import io.github.pnoker.common.entity.Driver;
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DriverService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {

    @Resource
    private DriverMapper driverMapper;

    @Lazy
    @Resource
    private DeviceService deviceService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Driver add(Driver driver) {
        try {
            selectByServiceName(driver.getServiceName());
            throw new DuplicateException("The driver already exists");
        } catch (NotFoundException notFoundException) {
            if (driverMapper.insert(driver) > 0) {
                return driverMapper.selectById(driver.getId());
            }
            throw new ServiceException("The driver add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean delete(String id) {
        selectById(id);
        return driverMapper.deleteById(id) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Driver update(Driver driver) {
        selectById(driver.getId());
        driver.setUpdateTime(null);
        if (driverMapper.updateById(driver) > 0) {
            Driver select = driverMapper.selectById(driver.getId());
            driver.setServiceName(select.getServiceName());
            driver.setServiceHost(select.getServiceHost());
            driver.setServicePort(select.getServicePort());
            return select;
        }
        throw new ServiceException("The driver update failed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Driver selectById(String id) {
        Driver driver = driverMapper.selectById(id);
        if (null == driver) {
            throw new NotFoundException();
        }
        return driver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Driver selectByDeviceId(String deviceId) {
        Device device = deviceService.selectById(deviceId);
        return selectById(device.getDriverId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Driver selectByServiceName(String serviceName) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        queryWrapper.eq(Driver::getServiceName, serviceName);
        Driver driver = driverMapper.selectOne(queryWrapper);
        if (null == driver) {
            throw new NotFoundException();
        }
        return driver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Driver selectByHostPort(DriverTypeFlagEnum type, String host, Integer port, String tenantId) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        queryWrapper.eq(Driver::getDriverTypeFlag, type);
        queryWrapper.eq(Driver::getServiceHost, host);
        queryWrapper.eq(Driver::getServicePort, port);
        queryWrapper.eq(Driver::getTenantId, tenantId);
        Driver driver = driverMapper.selectOne(queryWrapper);
        if (null == driver) {
            throw new NotFoundException();
        }
        return driver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Driver> selectByIds(Set<String> ids) {
        List<Driver> drivers = driverMapper.selectBatchIds(ids);
        if (CollUtil.isEmpty(drivers)) {
            throw new NotFoundException();
        }
        return drivers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Driver> selectByProfileId(String profileId) {
        List<Device> devices = deviceService.selectByProfileId(profileId);
        Set<String> driverIds = devices.stream().map(Device::getDriverId).collect(Collectors.toSet());
        return selectByIds(driverIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Driver> list(DriverDto driverDto) {
        if (null == driverDto.getPage()) {
            driverDto.setPage(new Pages());
        }
        return driverMapper.selectPage(driverDto.getPage().convert(), fuzzyQuery(driverDto));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<Driver> fuzzyQuery(DriverDto driverDto) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        if (ObjectUtil.isNotNull(driverDto)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(driverDto.getDriverName()), Driver::getDriverName, driverDto.getDriverName());
            queryWrapper.like(CharSequenceUtil.isNotBlank(driverDto.getServiceName()), Driver::getServiceName, driverDto.getServiceName());
            queryWrapper.like(CharSequenceUtil.isNotBlank(driverDto.getServiceHost()), Driver::getServiceHost, driverDto.getServiceHost());
            queryWrapper.eq(ObjectUtil.isNotNull(driverDto.getServicePort()), Driver::getServicePort, driverDto.getServicePort());
            if (ObjectUtil.isNull(driverDto.getDriverTypeFlag())) {
                driverDto.setDriverTypeFlag(DriverTypeFlagEnum.DRIVER);
            }
            queryWrapper.like(Driver::getDriverTypeFlag, driverDto.getDriverTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotNull(driverDto.getEnableFlag()), Driver::getEnableFlag, driverDto.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(driverDto.getTenantId()), Driver::getTenantId, driverDto.getTenantId());
        }
        return queryWrapper;
    }

}
