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

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.ProfileBindMapper;
import com.dc3.center.manager.mapper.ProfileMapper;
import com.dc3.center.manager.service.ProfileBindService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.ProfileBindDto;
import com.dc3.common.exception.DuplicateException;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Profile;
import com.dc3.common.model.ProfileBind;
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
 * ProfileBindService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class ProfileBindServiceImpl implements ProfileBindService {

    @Resource
    private ProfileMapper profileMapper;
    @Resource
    private ProfileBindMapper profileBindMapper;

    @Override
    @Caching(
            put = {@CachePut(value = Common.Cache.PROFILE_BIND + Common.Cache.ID, key = "#profileBind.id", condition = "#result!=null")},
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public ProfileBind add(ProfileBind profileBind) {
        try {
            selectByDeviceIdAndProfileId(profileBind.getDeviceId(), profileBind.getProfileId());
            throw new DuplicateException("The profile bind already exists");
        } catch (NotFoundException notFoundException) {
            if (profileBindMapper.insert(profileBind) > 0) {
                return profileBindMapper.selectById(profileBind.getId());
            }
            throw new ServiceException("The profile bind add failed");
        }
    }

    @Override
    public List<ProfileBind> addByDeviceId(Long deviceId, Set<Long> profileIds) {
        List<ProfileBind> profileBinds = new ArrayList<>();
        if (null != profileIds) {
            profileIds.forEach(profileId -> {
                Profile profile = profileMapper.selectById(profileId);
                if (ObjectUtil.isNotNull(profile)) {
                    ProfileBind profileBind = add(new ProfileBind(profileId, deviceId));
                    profileBinds.add(profileBind);
                }
            });
        }
        return profileBinds;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        selectById(id);
        return profileBindMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean deleteByDeviceId(Long deviceId) {
        ProfileBindDto profileBindDto = new ProfileBindDto();
        profileBindDto.setDeviceId(deviceId);
        return profileBindMapper.delete(fuzzyQuery(profileBindDto)) > 0;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean deleteByProfileIdAndDeviceId(Long deviceId, Long profileId) {
        ProfileBindDto profileBindDto = new ProfileBindDto();
        profileBindDto.setProfileId(profileId);
        profileBindDto.setDeviceId(deviceId);
        return profileBindMapper.delete(fuzzyQuery(profileBindDto)) > 0;
    }

    @Override
    @Caching(
            put = {@CachePut(value = Common.Cache.PROFILE_BIND + Common.Cache.ID, key = "#profileBind.id", condition = "#result!=null")},
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE_BIND + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public ProfileBind update(ProfileBind profileBind) {
        selectById(profileBind.getId());
        profileBind.setUpdateTime(null);
        if (profileBindMapper.updateById(profileBind) > 0) {
            return profileBindMapper.selectById(profileBind.getId());
        }
        throw new ServiceException("The profile bind update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_BIND + Common.Cache.ID, key = "#id", unless = "#result==null")
    public ProfileBind selectById(Long id) {
        ProfileBind profileBind = profileBindMapper.selectById(id);
        if (null == profileBind) {
            throw new NotFoundException("The profile bind does not exist");
        }
        return profileBind;
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID + Common.Cache.PROFILE_ID, key = "#deviceId+'.'+#profileId", unless = "#result==null")
    public ProfileBind selectByDeviceIdAndProfileId(Long deviceId, Long profileId) {
        ProfileBindDto profileBindDto = new ProfileBindDto();
        profileBindDto.setDeviceId(deviceId);
        profileBindDto.setProfileId(profileId);
        ProfileBind profileBind = profileBindMapper.selectOne(fuzzyQuery(profileBindDto));
        if (null == profileBind) {
            throw new NotFoundException("The profile bind does not exist");
        }
        return profileBind;
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_BIND + Common.Cache.PROFILE_ID, key = "#profileId", unless = "#result==null")
    public Set<Long> selectDeviceIdByProfileId(Long profileId) {
        ProfileBindDto profileBindDto = new ProfileBindDto();
        profileBindDto.setProfileId(profileId);
        List<ProfileBind> profileBinds = profileBindMapper.selectList(fuzzyQuery(profileBindDto));
        return profileBinds.stream().map(ProfileBind::getDeviceId).collect(Collectors.toSet());
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_BIND + Common.Cache.DEVICE_ID, key = "#deviceId", unless = "#result==null")
    public Set<Long> selectProfileIdByDeviceId(Long deviceId) {
        ProfileBindDto profileBindDto = new ProfileBindDto();
        profileBindDto.setDeviceId(deviceId);
        List<ProfileBind> profileBinds = profileBindMapper.selectList(fuzzyQuery(profileBindDto));
        return profileBinds.stream().map(ProfileBind::getProfileId).collect(Collectors.toSet());
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_BIND + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<ProfileBind> list(ProfileBindDto profileBindDto) {
        if (!Optional.ofNullable(profileBindDto.getPage()).isPresent()) {
            profileBindDto.setPage(new Pages());
        }
        return profileBindMapper.selectPage(profileBindDto.getPage().convert(), fuzzyQuery(profileBindDto));
    }

    @Override
    public LambdaQueryWrapper<ProfileBind> fuzzyQuery(ProfileBindDto profileBindDto) {
        LambdaQueryWrapper<ProfileBind> queryWrapper = Wrappers.<ProfileBind>query().lambda();
        if (null != profileBindDto) {
            if (null != profileBindDto.getProfileId()) {
                queryWrapper.eq(ProfileBind::getProfileId, profileBindDto.getProfileId());
            }
            if (null != profileBindDto.getDeviceId()) {
                queryWrapper.eq(ProfileBind::getDeviceId, profileBindDto.getDeviceId());
            }
        }
        return queryWrapper;
    }

}
