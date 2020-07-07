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
import com.dc3.center.manager.service.DeviceService;
import com.dc3.center.manager.service.DriverService;
import com.dc3.center.manager.service.ProfileService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DriverDto;
import com.dc3.common.dto.ProfileDto;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Device;
import com.dc3.common.model.Driver;
import com.dc3.common.model.Profile;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * <p>DriverService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {

    @Resource
    private EurekaClient eurekaClient;
    @Resource
    private DriverService driverService;
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
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Driver add(Driver driver) {
        Driver select = selectByServiceName(driver.getName());
        Optional.ofNullable(select).ifPresent(d -> {
            throw new ServiceException("The driver already exists");
        });
        if (driverMapper.insert(driver) > 0) {
            return driverMapper.selectById(driver.getId());
        }
        throw new ServiceException("The driver add failed");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.SERVICE_NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.HOST_PORT, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        ProfileDto profileDto = new ProfileDto();
        profileDto.setDriverId(id);
        Page<Profile> profilePage = profileService.list(profileDto);
        if (profilePage.getTotal() > 0) {
            throw new ServiceException("The driver already bound by the profile");
        }
        //todo 删除driver需要把它的属性和配置也一同删除
        Driver driver = selectById(id);
        if (null == driver) {
            throw new ServiceException("The driver does not exist");
        }
        return driverMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DRIVER + Common.Cache.ID, key = "#driver.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER + Common.Cache.SERVICE_NAME, key = "#driver.serviceName", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER + Common.Cache.HOST_PORT, key = "#driver.host+'.'+#driver.port", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Driver update(Driver driver) {
        Driver temp = selectById(driver.getId());
        if (null == temp) {
            throw new ServiceException("The driver does not exist");
        }
        driver.setUpdateTime(null);
        if (driverMapper.updateById(driver) > 0) {
            Driver select = driverMapper.selectById(driver.getId());
            driver.setServiceName(select.getServiceName());
            return select;
        }
        throw new ServiceException("The driver update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Driver selectById(Long id) {
        return driverMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.SERVICE_NAME, key = "#serviceName", unless = "#result==null")
    public Driver selectByServiceName(String serviceName) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        queryWrapper.eq(Driver::getServiceName, serviceName);
        return driverMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.HOST_PORT, key = "#host+'.'+#port", unless = "#result==null")
    public Driver selectByHostPort(String host, Integer port) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        queryWrapper.eq(Driver::getHost, host);
        queryWrapper.eq(Driver::getPort, port);
        return driverMapper.selectOne(queryWrapper);
    }

    @Override
    public Driver selectByDeviceId(Long deviceId) {
        Device device = deviceService.selectById(deviceId);
        if (null != device) {
            Profile profile = profileService.selectById(device.getProfileId());
            if (null != profile) {
                return driverService.selectById(profile.getDriverId());
            }
        }
        return null;
    }

    @Override
    public Driver selectByProfileId(Long profileId) {
        Profile profile = profileService.selectById(profileId);
        if (null != profile) {
            return driverService.selectById(profile.getDriverId());
        }
        return null;
    }

    @Override
    public Map<String, Boolean> driverStatus(DriverDto driverDto) {
        Map<String, Boolean> driverStatusMap = new HashMap<>(16);

        Page<Driver> driverPage = list(driverDto);
        if (driverPage.getRecords().size() > 0) {
            List<String> services = new ArrayList<>();
            List<Application> applications = eurekaClient.getApplications().getRegisteredApplications();
            applications.forEach(application -> services.add(application.getName().toLowerCase()));

            driverPage.getRecords().forEach(driver -> {
                boolean status = false;
                if (services.contains(driver.getServiceName())) {
                    status = true;
                }
                driverStatusMap.put(driver.getServiceName(), status);
            });
        }
        return driverStatusMap;
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Driver> list(DriverDto driverDto) {
        if (!Optional.ofNullable(driverDto.getPage()).isPresent()) {
            driverDto.setPage(new Pages());
        }
        return driverMapper.selectPage(driverDto.getPage().convert(), fuzzyQuery(driverDto));
    }

    @Override
    public LambdaQueryWrapper<Driver> fuzzyQuery(DriverDto driverDto) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        Optional.ofNullable(driverDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Driver::getName, dto.getName());
            }
            if (StringUtils.isNotBlank(dto.getServiceName())) {
                queryWrapper.like(Driver::getServiceName, dto.getServiceName());
            }
            if (StringUtils.isNotBlank(dto.getHost())) {
                queryWrapper.like(Driver::getHost, dto.getHost());
            }
            Optional.ofNullable(dto.getPort()).ifPresent(port -> queryWrapper.eq(Driver::getPort, port));
        });
        return queryWrapper;
    }

}
