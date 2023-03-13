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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.ProfilePageQuery;
import io.github.pnoker.center.manager.mapper.ProfileMapper;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.center.manager.service.ProfileService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.ProfileTypeFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ProfileService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    @Resource
    private ProfileMapper profileMapper;

    @Resource
    private PointService pointService;
    @Resource
    private ProfileBindService profileBindService;

    @Override
    public Profile add(Profile profile) {
        try {
            selectByNameAndType(profile.getProfileName(), profile.getProfileTypeFlag(), profile.getTenantId());
            throw new DuplicateException("The profile already exists");
        } catch (NotFoundException notFoundException1) {
            if (profileMapper.insert(profile) > 0) {
                return profileMapper.selectById(profile.getId());
            }
            throw new ServiceException("The profile add failed");
        }
    }


    @Override
    public Boolean delete(String id) {
        try {
            pointService.selectByProfileId(id);
            throw new ServiceException("The profile already bound by the point");
        } catch (NotFoundException notFoundException2) {
            selectById(id);
            return profileMapper.deleteById(id) > 0;
        }
    }

    @Override
    public Profile update(Profile profile) {
        selectById(profile.getId());
        profile.setUpdateTime(null);
        if (profileMapper.updateById(profile) > 0) {
            return profileMapper.selectById(profile.getId());
        }
        throw new ServiceException("The profile update failed");
    }

    @Override
    public Profile selectById(String id) {
        Profile profile = profileMapper.selectById(id);
        if (null == profile) {
            throw new NotFoundException();
        }
        return profile;
    }

    @Override
    public Profile selectByNameAndType(String name, ProfileTypeFlagEnum type, String tenantId) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        queryWrapper.eq(Profile::getProfileName, name);
        queryWrapper.eq(Profile::getProfileTypeFlag, type);
        queryWrapper.eq(Profile::getTenantId, tenantId);
        queryWrapper.last("limit 1");
        Profile profile = profileMapper.selectOne(queryWrapper);
        if (null == profile) {
            throw new NotFoundException();
        }
        return profile;
    }

    @Override
    public List<Profile> selectByIds(Set<String> ids) {
        List<Profile> profiles = new ArrayList<>();
        if (CollUtil.isNotEmpty(ids)) {
            profiles = profileMapper.selectBatchIds(ids);
        }
        return profiles;
    }

    @Override
    public List<Profile> selectByDeviceId(String deviceId) {
        Set<String> profileIds = profileBindService.selectProfileIdsByDeviceId(deviceId);
        if (CollUtil.isNotEmpty(profileIds)) {
            return selectByIds(profileIds);
        }
        return new ArrayList<>();
    }

    @Override
    public Page<Profile> list(ProfilePageQuery profilePageQuery) {
        if (ObjectUtil.isNull(profilePageQuery.getPage())) {
            profilePageQuery.setPage(new Pages());
        }
        return profileMapper.selectPageWithDevice(profilePageQuery.getPage().convert(), customFuzzyQuery(profilePageQuery), profilePageQuery.getDeviceId());
    }

    @Override
    public LambdaQueryWrapper<Profile> fuzzyQuery(ProfilePageQuery profilePageQuery) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        if (ObjectUtil.isNotEmpty(profilePageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(profilePageQuery.getProfileName()), Profile::getProfileName, profilePageQuery.getProfileName());
            queryWrapper.eq(ObjectUtil.isNotEmpty(profilePageQuery.getProfileShareFlag()), Profile::getProfileShareFlag, profilePageQuery.getProfileShareFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(profilePageQuery.getEnableFlag()), Profile::getEnableFlag, profilePageQuery.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotBlank(profilePageQuery.getTenantId()), Profile::getTenantId, profilePageQuery.getTenantId());
        }
        return queryWrapper;
    }

    public LambdaQueryWrapper<Profile> customFuzzyQuery(ProfilePageQuery profilePageQuery) {
        QueryWrapper<Profile> queryWrapper = Wrappers.query();
        queryWrapper.eq("dp.deleted", 0);
        if (ObjectUtil.isNotNull(profilePageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(profilePageQuery.getProfileName()), "dp.profile_name", profilePageQuery.getProfileName());
            queryWrapper.eq(ObjectUtil.isNotNull(profilePageQuery.getProfileCode()), "dp.profile_code", profilePageQuery.getProfileCode());
            queryWrapper.eq(ObjectUtil.isNotNull(profilePageQuery.getProfileShareFlag()), "dp.profile_share_flag", profilePageQuery.getProfileShareFlag());
            queryWrapper.eq(ObjectUtil.isNotNull(profilePageQuery.getEnableFlag()), "dp.enable_flag", profilePageQuery.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(profilePageQuery.getTenantId()), "dp.tenant_id", profilePageQuery.getTenantId());
        }
        return queryWrapper.lambda();
    }

}
