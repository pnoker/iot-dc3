/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.center.manager.service.ProfileService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeFlagEnum;
import io.github.pnoker.common.exception.*;
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
    @Resource
    private NotifyService notifyService;

    @Override
    public void add(Profile entityBO) {
        try {
            selectByNameAndType(entityBO.getProfileName(), entityBO.getProfileTypeFlag(), entityBO.getTenantId());
            throw new DuplicateException("The profile already exists");
        } catch (NotFoundException notFoundException1) {
            if (profileMapper.insert(entityBO) < 1) {
                throw new AddException("The profile {} add failed", entityBO.getProfileName());
            }
        }
    }


    @Override
    public void delete(Long id) {
        try {
            pointService.selectByProfileId(id);
            throw new ServiceException("The profile already bound by the point");
        } catch (NotFoundException notFoundException2) {
            Profile profile = get(id);
            if (ObjectUtil.isNull(profile)) {
                throw new NotFoundException("The profile does not exist");
            }

            if (profileMapper.deleteById(id) < 1) {
                throw new DeleteException("The profile delete failed");
            }

            notifyService.notifyDriverProfile(MetadataCommandTypeEnum.DELETE, profile);
        }
    }

    @Override
    public void update(Profile entityBO) {
        get(entityBO.getId());
        entityBO.setOperateTime(null);
        if (profileMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The profile update failed");
        }

        Profile update = profileMapper.selectById(entityBO.getId());
        notifyService.notifyDriverProfile(MetadataCommandTypeEnum.UPDATE, update);
    }

    @Override
    public Profile get(Long id) {
        Profile profile = profileMapper.selectById(id);
        if (ObjectUtil.isNull(profile)) {
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
        if (ObjectUtil.isNull(profile)) {
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
    public Long count() {
        return profileMapper.selectCount(new QueryWrapper<>());
    }

    @Override
    public Page<Profile> list(ProfilePageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return profileMapper.selectPageWithDevice(entityQuery.getPage().page(), customFuzzyQuery(entityQuery), entityQuery.getDeviceId());
    }

    private LambdaQueryWrapper<Profile> fuzzyQuery(ProfilePageQuery query) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        if (ObjectUtil.isNotEmpty(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getProfileName()), Profile::getProfileName, query.getProfileName());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getProfileShareFlag()), Profile::getProfileShareFlag, query.getProfileShareFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getEnableFlag()), Profile::getEnableFlag, query.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getTenantId()), Profile::getTenantId, query.getTenantId());
        }
        return queryWrapper;
    }

    private LambdaQueryWrapper<Profile> customFuzzyQuery(ProfilePageQuery profilePageQuery) {
        QueryWrapper<Profile> queryWrapper = Wrappers.query();
        queryWrapper.eq("dp.deleted", 0);
        if (ObjectUtil.isNotNull(profilePageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(profilePageQuery.getProfileName()), "dp.profile_name", profilePageQuery.getProfileName());
            queryWrapper.eq(ObjectUtil.isNotNull(profilePageQuery.getProfileCode()), "dp.profile_code", profilePageQuery.getProfileCode());
            queryWrapper.eq(ObjectUtil.isNotNull(profilePageQuery.getProfileShareFlag()), "dp.profile_share_flag", profilePageQuery.getProfileShareFlag());
            queryWrapper.eq(ObjectUtil.isNotNull(profilePageQuery.getEnableFlag()), "dp.enable_flag", profilePageQuery.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(profilePageQuery.getTenantId()), "dp.tenant_id", profilePageQuery.getTenantId());
        }
        return queryWrapper.lambda();
    }

}
