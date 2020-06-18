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
import com.dc3.center.manager.mapper.ProfileMapper;
import com.dc3.center.manager.service.DeviceService;
import com.dc3.center.manager.service.DriverService;
import com.dc3.center.manager.service.PointService;
import com.dc3.center.manager.service.ProfileService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DeviceDto;
import com.dc3.common.dto.PointDto;
import com.dc3.common.dto.ProfileDto;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Device;
import com.dc3.common.model.Driver;
import com.dc3.common.model.Point;
import com.dc3.common.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * <p>ProfileService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    @Resource
    private DriverService driverService;
    @Resource
    private DeviceService deviceService;
    @Resource
    private PointService pointService;
    @Resource
    private ProfileMapper profileMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.PROFILE + Common.Cache.ID, key = "#profile.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.PROFILE + Common.Cache.NAME, key = "#profile.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Profile add(Profile profile) {
        Driver driver = driverService.selectById(profile.getDriverId());
        if (null == driver) {
            throw new ServiceException("The driver does not exist");
        }
        Profile select = selectByName(profile.getName());
        if (null != select) {
            throw new ServiceException("The profile already exists");
        }
        if (profileMapper.insert(profile) > 0) {
            return profileMapper.selectById(profile.getId());
        }
        throw new ServiceException("The profile add failed");
    }


    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        DeviceDto deviceDto = new DeviceDto();
        deviceDto.setProfileId(id);
        Page<Device> devicePage = deviceService.list(deviceDto);
        if (devicePage.getTotal() > 0) {
            throw new ServiceException("The profile already bound by the device");
        }

        PointDto pointDto = new PointDto();
        pointDto.setProfileId(id);
        Page<Point> pointPage = pointService.list(pointDto);
        if (pointPage.getTotal() > 0) {
            throw new ServiceException("The profile already bound by the point");
        }
        Profile profile = selectById(id);
        if (null == profile) {
            throw new ServiceException("The profile does not exist");
        }
        return profileMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.PROFILE + Common.Cache.ID, key = "#profile.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.PROFILE + Common.Cache.NAME, key = "#profile.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Profile update(Profile profile) {
        Profile temp = selectById(profile.getId());
        if (null == temp) {
            throw new ServiceException("The profile does not exist");
        }
        profile.setUpdateTime(null);
        if (profileMapper.updateById(profile) > 0) {
            Profile select = profileMapper.selectById(profile.getId());
            profile.setName(select.getName());
            return select;
        }
        throw new ServiceException("The profile update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Profile selectById(Long id) {
        return profileMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE + Common.Cache.NAME, key = "#name", unless = "#result==null")
    public Profile selectByName(String name) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        queryWrapper.eq(Profile::getName, name);
        return profileMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Profile> list(ProfileDto profileDto) {
        if (!Optional.ofNullable(profileDto.getPage()).isPresent()) {
            profileDto.setPage(new Pages());
        }
        return profileMapper.selectPage(profileDto.getPage().convert(), fuzzyQuery(profileDto));
    }

    @Override
    public LambdaQueryWrapper<Profile> fuzzyQuery(ProfileDto profileDto) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        Optional.ofNullable(profileDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Profile::getName, dto.getName());
            }
            if (null != dto.getShare()) {
                queryWrapper.eq(Profile::getShare, dto.getShare());
            }
            if (null != dto.getDriverId()) {
                queryWrapper.eq(Profile::getDriverId, dto.getDriverId());
            }
        });
        return queryWrapper;
    }

}
