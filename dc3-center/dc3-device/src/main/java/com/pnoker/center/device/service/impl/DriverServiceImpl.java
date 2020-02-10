/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.center.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.DriverDto;
import com.pnoker.common.dto.ProfileDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.Driver;
import com.pnoker.common.model.Profile;
import com.pnoker.center.device.mapper.DriverMapper;
import com.pnoker.center.device.service.DriverService;
import com.pnoker.center.device.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>驱动服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {

    @Resource
    private ProfileService profileService;

    @Resource
    private DriverMapper driverMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DRIVER_ID, key = "#driver.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER_SERVICE_NAME, key = "#driver.serviceName", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER_DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Driver add(Driver driver) {
        Driver select = selectByServiceName(driver.getName());
        if (null != select) {
            throw new ServiceException("driver already exists");
        }
        if (driverMapper.insert(driver) > 0) {
            return driverMapper.selectById(driver.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER_ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER_SERVICE_NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER_DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER_LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        ProfileDto profileDto = new ProfileDto();
        profileDto.setDriverId(id);
        Page<Profile> profilePage = profileService.list(profileDto);
        if (profilePage.getTotal() > 0) {
            throw new ServiceException("driver already bound by the profile");
        }
        return driverMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DRIVER_ID, key = "#driver.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER_SERVICE_NAME, key = "#driver.serviceName", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER_DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Driver update(Driver driver) {
        driver.setUpdateTime(null);
        if (driverMapper.updateById(driver) > 0) {
            Driver select = selectById(driver.getId());
            driver.setName(select.getName());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_ID, key = "#id", unless = "#result==null")
    public Driver selectById(Long id) {
        return driverMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_SERVICE_NAME, key = "#serviceName", unless = "#result==null")
    public Driver selectByServiceName(String serviceName) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        queryWrapper.eq(Driver::getServiceName, serviceName);
        return driverMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Driver> list(DriverDto driverDto) {
        if (!Optional.ofNullable(driverDto.getPage()).isPresent()) {
            driverDto.setPage(new Pages());
        }
        return driverMapper.selectPage(driverDto.getPage().convert(), fuzzyQuery(driverDto));
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_DIC, key = "'dirver_dic'", unless = "#result==null")
    public List<Dic> dictionary() {
        List<Dic> dicList = new ArrayList<>();
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        List<Driver> driverList = driverMapper.selectList(queryWrapper);
        for (Driver driver : driverList) {
            Dic driverDic = new Dic().setLabel(driver.getName()).setValue(driver.getId());
            dicList.add(driverDic);
        }
        return dicList;
    }

    @Override
    public LambdaQueryWrapper<Driver> fuzzyQuery(DriverDto driverDto) {
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        Optional.ofNullable(driverDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Driver::getName, dto.getName());
            }
        });
        return queryWrapper;
    }

}
