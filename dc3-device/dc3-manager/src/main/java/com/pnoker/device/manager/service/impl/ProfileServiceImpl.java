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

package com.pnoker.device.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.ProfileDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Profile;
import com.pnoker.device.manager.mapper.ProfileMapper;
import com.pnoker.device.manager.service.ProfileService;
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
 * 模板服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {
    @Resource
    private ProfileMapper profileMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.PROFILE_ID, key = "#profile.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.PROFILE_NAME, key = "#profile.name", condition = "#result!=null")
            },
            evict = {@CacheEvict(value = Common.Cache.PROFILE_LIST, allEntries = true, condition = "#result!=null")}
    )
    public Profile add(Profile profile) {
        Profile select = selectByName(profile.getName());
        if (null != select) {
            throw new ServiceException("位号已存在");
        }
        if (profileMapper.insert(profile) > 0) {
            return profileMapper.selectById(profile.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE_ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        return profileMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.PROFILE_ID, key = "#profile.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.PROFILE_NAME, key = "#profile.name", condition = "#result!=null")
            },
            evict = {@CacheEvict(value = Common.Cache.PROFILE_LIST, allEntries = true, condition = "#result==true")}
    )
    public Profile update(Profile profile) {
        if (profileMapper.updateById(profile) > 0) {
            Profile select = selectById(profile.getId());
            profile.setName(select.getName());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_ID, key = "#id", unless = "#result==null")
    public Profile selectById(Long id) {
        return profileMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_NAME, key = "#name", unless = "#result==null")
    public Profile selectByName(String name) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        queryWrapper.like(Profile::getName, name);
        return profileMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Profile> list(ProfileDto profileDto) {
        return profileMapper.selectPage(profileDto.getPage().convert(), fuzzyQuery(profileDto));
    }

    @Override
    public LambdaQueryWrapper<Profile> fuzzyQuery(ProfileDto profileDto) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        Optional.ofNullable(profileDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Profile::getName, dto.getName());
            }
        });
        return queryWrapper;
    }

}
