/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.DriverMapper;
import com.dc3.center.manager.service.*;
import com.dc3.common.bean.Pages;
import com.dc3.common.bean.driver.DriverRegister;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DriverDto;
import com.dc3.common.exception.DuplicateException;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.*;
import com.dc3.common.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>DriverService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private DriverAttributeService driverAttributeService;
    @Resource
    private DriverInfoService driverInfoService;
    @Resource
    private PointAttributeService pointAttributeService;
    @Resource
    private PointInfoService pointInfoService;
    @Resource
    private DeviceService deviceService;
    @Resource
    private ProfileService profileService;
    @Resource
    private DriverMapper driverMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DRIVER + Common.Cache.ID, key = "#driver.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER + Common.Cache.SERVICE_NAME, key = "#driver.serviceName", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER + Common.Cache.HOST_PORT, key = "#driver.host+'.'+#driver.port", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.DEVICE_ID, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Driver add(Driver driver) {
        try {
            selectByServiceName(driver.getName());
            throw new DuplicateException("The driver already exists");
        } catch (NotFoundException notFoundException) {
            if (driverMapper.insert(driver) > 0) {
                return driverMapper.selectById(driver.getId());
            }
            throw new ServiceException("The driver add failed");
        }
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.DEVICE_ID, allEntries = true, condition = "#result!=true"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result!=true"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.SERVICE_NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.HOST_PORT, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        try {
            profileService.selectByDriverId(id);
            throw new ServiceException("The driver already bound by the profile");
        } catch (NotFoundException notFoundException1) {
            selectById(id);
            //TODO 需要删除驱动下所有的模板，设备，位号
            return driverMapper.deleteById(id) > 0;
        }
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DRIVER + Common.Cache.ID, key = "#driver.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER + Common.Cache.SERVICE_NAME, key = "#driver.serviceName", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER + Common.Cache.HOST_PORT, key = "#driver.host+'.'+#driver.port", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.DEVICE_ID, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
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
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Driver selectById(Long id) {
        Driver driver = driverMapper.selectById(id);
        if (null == driver) {
            throw new NotFoundException("The driver does not exist");
        }
        return driver;
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.DEVICE_ID, key = "#deviceId", unless = "#result==null")
    public Driver selectByDeviceId(Long deviceId) {
        Device device = deviceService.selectById(deviceId);
        Profile profile = profileService.selectById(device.getProfileId());
        return selectById(profile.getDriverId());
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.PROFILE_ID, key = "#profileId", unless = "#result==null")
    public Driver selectByProfileId(Long profileId) {
        Profile profile = profileService.selectById(profileId);
        return selectById(profile.getDriverId());
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.SERVICE_NAME, key = "#serviceName", unless = "#result==null")
    public Driver selectByServiceName(String serviceName) {
        DriverDto driverDto = new DriverDto();
        driverDto.setServiceName(serviceName);
        Driver driver = driverMapper.selectOne(fuzzyQuery(driverDto));
        if (null == driver) {
            throw new NotFoundException("The driver does not exist");
        }
        return driver;
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.HOST_PORT, key = "#host+'.'+#port", unless = "#result==null")
    public Driver selectByHostPort(String host, Integer port) {
        DriverDto driverDto = new DriverDto();
        driverDto.setHost(host);
        driverDto.setPort(port);
        Driver driver = driverMapper.selectOne(fuzzyQuery(driverDto));
        if (null == driver) {
            throw new NotFoundException("The driver does not exist");
        }
        return driver;
    }

    //TODO 合并到list中
    @Override
    public Map<String, Boolean> driverStatus(DriverDto driverDto) {
        Map<String, Boolean> driverStatusMap = new HashMap<>(16);

        Page<Driver> driverPage = list(driverDto);
        if (driverPage.getRecords().size() > 0) {
            driverPage.getRecords().forEach(driver -> {
                String key = Common.Cache.DRIVER_STATUS_KEY_PREFIX + driver.getServiceName();
                String value = redisUtil.getKey(key, String.class);
                value = null != value ? value : Common.Driver.Status.OFFLINE;
                // todo driver status
                boolean status = value.equals(Common.Driver.Status.ONLINE);
                driverStatusMap.put(driver.getServiceName(), status);
            });
        }
        return driverStatusMap;
    }

    @Override
    public void driverRegister(DriverRegister driverRegister) {
        // register driver
        Driver driver = driverRegister.getDriver();
        log.info("Register driver {}", driver);
        try {
            Driver byServiceName = selectByServiceName(driver.getServiceName());
            log.debug("Driver already registered, updating {} ", driver);
            driver.setId(byServiceName.getId());
            driver = update(driver);
        } catch (NotFoundException notFoundException1) {
            log.debug("Driver does not registered, adding {} ", driver);
            try {
                Driver byHostPort = selectByHostPort(driver.getHost(), driver.getPort());
                throw new ServiceException("The port(" + driver.getPort() + ") is already occupied by driver(" + byHostPort.getName() + "/" + byHostPort.getServiceName() + ")");
            } catch (NotFoundException notFoundException2) {
                driver = add(driver);
            }
        }

        //register driver attribute
        Map<String, DriverAttribute> newDriverAttributeMap = new HashMap<>(8);
        if (null != driverRegister.getDriverAttributes() && driverRegister.getDriverAttributes().size() > 0) {
            driverRegister.getDriverAttributes().forEach(driverAttribute -> newDriverAttributeMap.put(driverAttribute.getName(), driverAttribute));
        }

        Map<String, DriverAttribute> oldDriverAttributeMap = new HashMap<>(8);
        try {
            List<DriverAttribute> byDriverId = driverAttributeService.selectByDriverId(driver.getId());
            byDriverId.forEach(driverAttribute -> oldDriverAttributeMap.put(driverAttribute.getName(), driverAttribute));
        } catch (NotFoundException ignored) {
        }

        for (String name : newDriverAttributeMap.keySet()) {
            DriverAttribute info = newDriverAttributeMap.get(name).setDriverId(driver.getId());
            if (oldDriverAttributeMap.containsKey(name)) {
                info.setId(oldDriverAttributeMap.get(name).getId());
                log.debug("Driver attribute registered, updating: {}", info);
                driverAttributeService.update(info);
            } else {
                log.debug("Driver attribute does not registered, adding: {}", info);
                driverAttributeService.add(info);
            }
        }

        for (String name : oldDriverAttributeMap.keySet()) {
            if (!newDriverAttributeMap.containsKey(name)) {
                try {
                    driverInfoService.selectByAttributeId(oldDriverAttributeMap.get(name).getId());
                    throw new ServiceException("The driver attribute(" + name + ") used by driver info and cannot be deleted");
                } catch (NotFoundException notFoundException) {
                    log.debug("Driver attribute is redundant, deleting: {}", oldDriverAttributeMap.get(name));
                    driverAttributeService.delete(oldDriverAttributeMap.get(name).getId());
                }
            }
        }

        // register point attribute
        Map<String, PointAttribute> newPointAttributeMap = new HashMap<>(8);
        if (null != driverRegister.getPointAttributes() && driverRegister.getPointAttributes().size() > 0) {
            driverRegister.getPointAttributes().forEach(pointAttribute -> newPointAttributeMap.put(pointAttribute.getName(), pointAttribute));
        }

        Map<String, PointAttribute> oldPointAttributeMap = new HashMap<>(8);
        try {
            List<PointAttribute> byDriverId = pointAttributeService.selectByDriverId(driver.getId());
            byDriverId.forEach(pointAttribute -> oldPointAttributeMap.put(pointAttribute.getName(), pointAttribute));
        } catch (NotFoundException ignored) {
        }

        for (String name : newPointAttributeMap.keySet()) {
            PointAttribute attribute = newPointAttributeMap.get(name).setDriverId(driver.getId());
            if (oldPointAttributeMap.containsKey(name)) {
                attribute.setId(oldPointAttributeMap.get(name).getId());
                log.debug("Point attribute registered, updating: {}", attribute);
                pointAttributeService.update(attribute);
            } else {
                log.debug("Point attribute registered, adding: {}", attribute);
                pointAttributeService.add(attribute);
            }
        }

        for (String name : oldPointAttributeMap.keySet()) {
            if (!newPointAttributeMap.containsKey(name)) {
                try {
                    pointInfoService.selectByAttributeId(oldPointAttributeMap.get(name).getId());
                    throw new ServiceException("The point attribute(" + name + ") used by point info and cannot be deleted");
                } catch (NotFoundException notFoundException1) {
                    log.debug("Point attribute is redundant, deleting: {}", oldPointAttributeMap.get(name));
                    pointAttributeService.delete(oldPointAttributeMap.get(name).getId());
                }
            }
        }
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Driver> list(DriverDto driverDto) {
        if (null == driverDto.getPage()) {
            driverDto.setPage(new Pages());
        }
        return driverMapper.selectPage(driverDto.getPage().convert(), fuzzyQuery(driverDto));
    }

    @Override
    public LambdaQueryWrapper<Driver> fuzzyQuery(DriverDto driverDto) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        Optional.ofNullable(driverDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getServiceName())) {
                queryWrapper.like(Driver::getServiceName, dto.getServiceName());
            }
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Driver::getName, dto.getName());
            }
            if (StringUtils.isNotBlank(dto.getHost())) {
                queryWrapper.like(Driver::getHost, dto.getHost());
            }
            if (null != dto.getPort()) {
                queryWrapper.eq(Driver::getPort, dto.getPort());
            }
        });
        return queryWrapper;
    }

}
