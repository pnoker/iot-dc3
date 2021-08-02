/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.ProfileMapper;
import com.dc3.center.manager.service.DeviceService;
import com.dc3.center.manager.service.PointService;
import com.dc3.center.manager.service.ProfileService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.ProfileDto;
import com.dc3.common.exception.DuplicateException;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>ProfileService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    @Resource
    private PointService pointService;
    @Resource
    private ProfileMapper profileMapper;
    @Resource
    private DeviceService deviceService;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.PROFILE + Common.Cache.ID, key = "#profile.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.PROFILE + Common.Cache.NAME, key = "#profile.name+'.'+#profile.tenantId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Profile add(Profile profile) {
        try {
            selectByName(profile.getName(), profile.getTenantId());
            throw new DuplicateException("The profile already exists");
        } catch (NotFoundException notFoundException1) {
            if (profileMapper.insert(profile) > 0) {
                Profile select = profileMapper.selectById(profile.getId());
                try {
                    select.setPointIds(pointService.selectByProfileId(profile.getId()).stream().map(Point::getId).collect(Collectors.toSet()));
                } catch (NotFoundException ignored) {
                }
                return select;
            }
            throw new ServiceException("The profile add failed");
        }
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
        try {
            pointService.selectByProfileId(id);
            throw new ServiceException("The profile already bound by the point");
        } catch (NotFoundException notFoundException2) {
            selectById(id);
            return profileMapper.deleteById(id) > 0;
        }
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.PROFILE + Common.Cache.ID, key = "#profile.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.PROFILE + Common.Cache.NAME, key = "#profile.name+'.'+#profile.tenantId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Profile update(Profile profile) {
        selectById(profile.getId());
        profile.setUpdateTime(null);
        if (profileMapper.updateById(profile) > 0) {
            Profile select = profileMapper.selectById(profile.getId());
            try {
                select.setPointIds(pointService.selectByProfileId(profile.getId()).stream().map(Point::getId).collect(Collectors.toSet()));
            } catch (NotFoundException ignored) {
            }
            profile.setName(select.getName());
            return select;
        }
        throw new ServiceException("The profile update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Profile selectById(Long id) {
        Profile profile = profileMapper.selectById(id);
        if (null == profile) {
            throw new NotFoundException("The profile does not exist");
        }
        try {
            profile.setPointIds(pointService.selectByProfileId(id).stream().map(Point::getId).collect(Collectors.toSet()));
        } catch (NotFoundException ignored) {
        }
        return profile;
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE + Common.Cache.NAME, key = "#name+'.'+#tenantId", unless = "#result==null")
    public Profile selectByName(String name, Long tenantId) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        queryWrapper.eq(Profile::getName, name);
        queryWrapper.eq(Profile::getTenantId, tenantId);
        Profile profile = profileMapper.selectOne(queryWrapper);
        if (null == profile) {
            throw new NotFoundException("The profile does not exist");
        }
        try {
            profile.setPointIds(pointService.selectByProfileId(profile.getId()).stream().map(Point::getId).collect(Collectors.toSet()));
        } catch (NotFoundException ignored) {
        }
        return profile;
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public List<Profile> selectByIds(Set<Long> ids) {
        List<Profile> profiles = profileMapper.selectBatchIds(ids);
        profiles.forEach(profile -> {
            try {
                profile.setPointIds(pointService.selectByProfileId(profile.getId()).stream().map(Point::getId).collect(Collectors.toSet()));
            } catch (NotFoundException ignored) {
            }
        });
        return profiles;
    }

    @Override
    public List<Profile> selectByDeviceId(Long deviceId) {
        Device device = deviceService.selectById(deviceId);
        return selectByIds(device.getProfileIds());
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Profile> list(ProfileDto profileDto) {
        if (!Optional.ofNullable(profileDto.getPage()).isPresent()) {
            profileDto.setPage(new Pages());
        }
        Page<Profile> page = profileMapper.selectPage(profileDto.getPage().convert(), fuzzyQuery(profileDto));
        page.getRecords().forEach(profile -> {
            try {
                profile.setPointIds(pointService.selectByProfileId(profile.getId()).stream().map(Point::getId).collect(Collectors.toSet()));
            } catch (NotFoundException ignored) {
            }
        });
        return page;
    }

    @Override
    public LambdaQueryWrapper<Profile> fuzzyQuery(ProfileDto profileDto) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        if (null != profileDto) {
            if (StrUtil.isNotBlank(profileDto.getName())) {
                queryWrapper.like(Profile::getName, profileDto.getName());
            }
            if (null != profileDto.getShare()) {
                queryWrapper.eq(Profile::getShare, profileDto.getShare());
            }
            if (null != profileDto.getEnable()) {
                queryWrapper.eq(Profile::getEnable, profileDto.getEnable());
            }
            if (null != profileDto.getTenantId()) {
                queryWrapper.eq(Profile::getTenantId, profileDto.getTenantId());
            }
        }
        return queryWrapper;
    }

}
