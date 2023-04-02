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

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.DriverPageQuery;
import io.github.pnoker.center.manager.mapper.DriverMapper;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Driver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
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
    public void add(Driver entityDO) {
        try {
            selectByServiceName(entityDO.getServiceName(), entityDO.getTenantId());
            throw new DuplicateException("The driver already exists");
        } catch (NotFoundException notFoundException) {
            driverMapper.insert(entityDO);
            throw new ServiceException("The driver add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String id) {
        selectById(id);
        driverMapper.deleteById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Driver entityDO) {
        selectById(entityDO.getId());
        entityDO.setOperateTime(null);
        if (driverMapper.updateById(entityDO) > 0) {
            Driver select = driverMapper.selectById(entityDO.getId());
            entityDO.setServiceName(select.getServiceName());
            entityDO.setServiceHost(select.getServiceHost());
        }
        throw new ServiceException("The driver update failed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Driver selectById(String id) {
        Driver driver = driverMapper.selectById(id);
        if (ObjectUtil.isNull(driver)) {
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
    public Driver selectByServiceName(String serviceName, String tenantId) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        queryWrapper.eq(Driver::getServiceName, serviceName);
        queryWrapper.eq(Driver::getTenantId, tenantId);
        queryWrapper.last("limit 1");
        Driver driver = driverMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(driver)) {
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
        queryWrapper.eq(Driver::getTenantId, tenantId);
        queryWrapper.last("limit 1");
        Driver driver = driverMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(driver)) {
            throw new NotFoundException();
        }
        return driver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Driver> selectByIds(Set<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return driverMapper.selectBatchIds(ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Driver> selectByProfileId(String profileId) {
        List<Device> devices = deviceService.selectByProfileId(profileId);
        Set<String> driverIds = devices.stream().map(Device::getDriverId).collect(Collectors.toSet());
        List<Driver> driverList = selectByIds(driverIds);
        if (CollUtil.isEmpty(driverList)) {
            return Collections.emptyList();
        }
        return driverList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Driver> list(DriverPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return driverMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    public LambdaQueryWrapper<Driver> fuzzyQuery(DriverPageQuery query) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getDriverName()), Driver::getDriverName, query.getDriverName());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getServiceName()), Driver::getServiceName, query.getServiceName());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getServiceHost()), Driver::getServiceHost, query.getServiceHost());
            queryWrapper.eq(ObjectUtil.isNotNull(query.getDriverTypeFlag()), Driver::getDriverTypeFlag, query.getDriverTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotNull(query.getEnableFlag()), Driver::getEnableFlag, query.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getTenantId()), Driver::getTenantId, query.getTenantId());
        }
        return queryWrapper;
    }

}
