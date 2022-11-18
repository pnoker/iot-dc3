/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.mapper.DriverMapper;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.common.bean.Pages;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.dto.DriverDto;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Driver;
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
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {

    @Resource
    private DriverMapper driverMapper;

    @Lazy
    @Resource
    private DeviceService deviceService;

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

    @Override
    public boolean delete(String id) {
        selectById(id);
        return driverMapper.deleteById(id) > 0;
    }

    @Override
    public Driver update(Driver driver) {
        selectById(driver.getId());
        driver.setUpdateTime(null);
        if (driverMapper.updateById(driver) > 0) {
            Driver select = driverMapper.selectById(driver.getId());
            driver.setServiceName(select.getServiceName()).setHost(select.getHost()).setPort(select.getPort());
            return select;
        }
        throw new ServiceException("The driver update failed");
    }

    @Override
    public Driver selectById(String id) {
        Driver driver = driverMapper.selectById(id);
        if (null == driver) {
            throw new NotFoundException("The driver does not exist");
        }
        return driver;
    }

    @Override
    public Driver selectByDeviceId(String deviceId) {
        Device device = deviceService.selectById(deviceId);
        return selectById(device.getDriverId());
    }

    @Override
    public Driver selectByServiceName(String serviceName) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        queryWrapper.eq(Driver::getServiceName, serviceName);
        Driver driver = driverMapper.selectOne(queryWrapper);
        if (null == driver) {
            throw new NotFoundException("The driver does not exist");
        }
        return driver;
    }

    @Override
    public Driver selectByHostPort(String type, String host, Integer port, String tenantId) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        queryWrapper.eq(Driver::getType, type);
        queryWrapper.eq(Driver::getHost, host);
        queryWrapper.eq(Driver::getPort, port);
        queryWrapper.eq(Driver::getTenantId, tenantId);
        Driver driver = driverMapper.selectOne(queryWrapper);
        if (null == driver) {
            throw new NotFoundException("The driver does not exist");
        }
        return driver;
    }

    @Override
    public List<Driver> selectByIds(Set<String> ids) {
        List<Driver> drivers = driverMapper.selectBatchIds(ids);
        if (CollectionUtil.isEmpty(drivers)) {
            throw new NotFoundException("The driver does not exist");
        }
        return drivers;
    }

    @Override
    public List<Driver> selectByProfileId(String profileId) {
        List<Device> devices = deviceService.selectByProfileId(profileId);
        Set<String> driverIds = devices.stream().map(Device::getDriverId).collect(Collectors.toSet());
        return selectByIds(driverIds);
    }

    @Override
    public Page<Driver> list(DriverDto driverDto) {
        if (null == driverDto.getPage()) {
            driverDto.setPage(new Pages());
        }
        return driverMapper.selectPage(driverDto.getPage().convert(), fuzzyQuery(driverDto));
    }

    @Override
    public LambdaQueryWrapper<Driver> fuzzyQuery(DriverDto driverDto) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        if (ObjectUtil.isNotNull(driverDto)) {
            queryWrapper.like(StrUtil.isNotEmpty(driverDto.getName()), Driver::getName, driverDto.getName());
            queryWrapper.like(StrUtil.isNotEmpty(driverDto.getServiceName()), Driver::getServiceName, driverDto.getServiceName());
            queryWrapper.like(StrUtil.isNotEmpty(driverDto.getHost()), Driver::getHost, driverDto.getHost());
            queryWrapper.eq(ObjectUtil.isNotNull(driverDto.getPort()), Driver::getPort, driverDto.getPort());
            if (StrUtil.isEmpty(driverDto.getType())) {
                driverDto.setType(CommonConstant.Driver.Type.DRIVER);
            }
            queryWrapper.like(Driver::getType, driverDto.getType());
            queryWrapper.eq(ObjectUtil.isNotNull(driverDto.getEnable()), Driver::getEnable, driverDto.getEnable());
            queryWrapper.eq(StrUtil.isNotEmpty(driverDto.getTenantId()), Driver::getTenantId, driverDto.getTenantId());
        }
        return queryWrapper;
    }

}
