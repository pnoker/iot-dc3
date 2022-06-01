/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.DeviceMapper;
import com.dc3.center.manager.mapper.ProfileMapper;
import com.dc3.center.manager.service.PointService;
import com.dc3.center.manager.service.ProfileService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.CacheConstant;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ProfileService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    @Resource
    private PointService pointService;

    @Resource
    private DeviceMapper deviceMapper;
    @Resource
    private ProfileMapper profileMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.ID, key = "#profile.id", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.NAME + CacheConstant.Suffix.TYPE, key = "#profile.name+'.'+#profile.type+'.'+#profile.tenantId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Profile add(Profile profile) {
        try {
            selectByNameAndType(profile.getName(), profile.getType(), profile.getTenantId());
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
                    @CacheEvict(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.NAME + CacheConstant.Suffix.TYPE, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(String id) {
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
                    @CachePut(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.ID, key = "#profile.id", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.NAME + CacheConstant.Suffix.TYPE, key = "#profile.name+'.'+#profile.type+'.'+#profile.tenantId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null")
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
    @Cacheable(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.ID, key = "#id", unless = "#result==null")
    public Profile selectById(String id) {
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
    @Cacheable(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.NAME + CacheConstant.Suffix.TYPE, key = "#name+'.'+#type+'.'+#tenantId", unless = "#result==null")
    public Profile selectByNameAndType(String name, Short type, String tenantId) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        queryWrapper.eq(Profile::getName, name);
        queryWrapper.eq(Profile::getType, type);
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
    @Cacheable(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public List<Profile> selectByIds(Set<String> ids) {
        List<Profile> profiles = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(ids)) {
            profiles = profileMapper.selectBatchIds(ids);
            profiles.forEach(profile -> {
                try {
                    profile.setPointIds(pointService.selectByProfileId(profile.getId()).stream().map(Point::getId).collect(Collectors.toSet()));
                } catch (NotFoundException ignored) {
                }
            });
        }
        return profiles;
    }

    @Override
    public List<Profile> selectByDeviceId(String deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (ObjectUtil.isNotNull(device)) {
            return selectByIds(device.getProfileIds());
        }
        return new ArrayList<>();
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
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
