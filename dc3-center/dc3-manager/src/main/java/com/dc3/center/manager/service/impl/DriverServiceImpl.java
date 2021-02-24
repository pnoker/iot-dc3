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
import com.dc3.common.bean.driver.DriverConfiguration;
import com.dc3.common.bean.driver.DriverRegister;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DriverDto;
import com.dc3.common.dto.ProfileDto;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.*;
import com.dc3.common.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private DriverService driverService;
    @Resource
    private DriverAttributeService driverAttributeService;
    @Resource
    private DriverInfoService driverInfoService;
    @Resource
    private BatchService batchService;
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
    @Resource
    private RabbitTemplate rabbitTemplate;

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
        if (null != selectByServiceName(driver.getName())) {
            throw new ServiceException("The driver already exists");
        }

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
        Driver driver = driverMapper.selectById(id);
        if (null == driver) {
            throw new ServiceException("The driver does not exist");
        }
        return driver;
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.SERVICE_NAME, key = "#serviceName", unless = "#result==null")
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
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.HOST_PORT, key = "#host+'.'+#port", unless = "#result==null")
    public Driver selectByHostPort(String host, Integer port) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        queryWrapper.eq(Driver::getHost, host);
        queryWrapper.eq(Driver::getPort, port);
        Driver driver = driverMapper.selectOne(queryWrapper);
        if (null == driver) {
            throw new NotFoundException("The driver does not exist");
        }
        return driver;
    }

    @Override
    public Driver selectByDeviceId(Long deviceId) {
        Device device = deviceService.selectById(deviceId);
        if (null != device) {
            Profile profile = profileService.selectById(device.getProfileId());
            if (null != profile) {
                Driver driver = driverService.selectById(profile.getDriverId());
                if (null == driver) {
                    throw new ServiceException("The driver does not exist");
                }
                return driver;
            }
        }
        throw new ServiceException("The driver does not exist");
    }

    @Override
    public Driver selectByProfileId(Long profileId) {
        Profile profile = profileService.selectById(profileId);
        if (null != profile) {
            Driver driver = driverService.selectById(profile.getDriverId());
            if (null == driver) {
                throw new ServiceException("The driver does not exist");
            }
            return driver;
        }
        throw new ServiceException("The driver does not exist");
    }

    @Override
    public Map<String, Boolean> driverStatus(DriverDto driverDto) {
        Map<String, Boolean> driverStatusMap = new HashMap<>(16);

        Page<Driver> driverPage = driverService.list(driverDto);
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
    public void syncDriverMetadata(DriverRegister driverRegister) {
        // register driver
        Driver driver = driverRegister.getDriver();
        try {
            Driver byServiceName = driverService.selectByServiceName(driver.getServiceName());
            log.info("Driver already registered, updating {} ", driver);
            driver.setId(byServiceName.getId());
            driver = driverService.update(driver);
        } catch (NotFoundException notFoundException1) {
            log.info("Driver does not registered, adding {} ", driver);
            try {
                Driver byHostPort = driverService.selectByHostPort(driver.getHost(), driver.getPort());
                throw new ServiceException("The port(" + driver.getPort() + ") is already occupied by driver(" + byHostPort.getName() + "/" + byHostPort.getServiceName() + ")");
            } catch (NotFoundException notFoundException2) {
                driver = driverService.add(driver);
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
                log.info("Driver attribute registered, updating: {}", info);
                driverAttributeService.update(info);
            } else {
                log.info("Driver attribute does not registered, adding: {}", info);
                driverAttributeService.add(info);
            }
        }

        for (String name : oldDriverAttributeMap.keySet()) {
            if (!newDriverAttributeMap.containsKey(name)) {
                try {
                    driverInfoService.selectByAttributeId(oldDriverAttributeMap.get(name).getId());
                    throw new ServiceException("The driver attribute(" + name + ") used by driver info and cannot be deleted");
                } catch (NotFoundException notFoundException) {
                    log.info("Driver attribute is redundant, deleting: {}", oldDriverAttributeMap.get(name));
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
                log.info("Point attribute registered, updating: {}", attribute);
                pointAttributeService.update(attribute);
            } else {
                log.info("Point attribute registered, adding: {}", attribute);
                pointAttributeService.add(attribute);
            }
        }

        for (String name : oldPointAttributeMap.keySet()) {
            if (!newPointAttributeMap.containsKey(name)) {
                try {
                    pointInfoService.selectByAttributeId(oldPointAttributeMap.get(name).getId());
                    throw new ServiceException("The point attribute(" + name + ") used by point info and cannot be deleted");
                } catch (NotFoundException notFoundException1) {
                    log.info("Point attribute is redundant, deleting: {}", oldPointAttributeMap.get(name));
                    pointAttributeService.delete(oldPointAttributeMap.get(name).getId());
                }
            }
        }

        DriverConfiguration driverConfiguration = new DriverConfiguration(
                Common.Driver.Type.METADATA,
                Common.Driver.Metadata.INIT,
                batchService.exportDriverMetadata(driver.getServiceName())
        );

        rabbitTemplate.convertAndSend(
                Common.Rabbit.TOPIC_EXCHANGE_CONFIGURATION,
                Common.Rabbit.ROUTING_DRIVER_CONFIGURATION_PREFIX + driver.getServiceName(),
                driverConfiguration
        );
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
