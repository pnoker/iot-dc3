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
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.ProfileMapper;
import com.dc3.center.manager.service.PointService;
import com.dc3.center.manager.service.ProfileBindService;
import com.dc3.center.manager.service.ProfileService;
import com.dc3.common.bean.Pages;
import com.dc3.common.dto.ProfileDto;
import com.dc3.common.exception.DuplicateException;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ProfileService Impl
 *
 * @author pnoker
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
    // 2022-07-30 检查：不通过，新增模板是没有添加位号的，为啥还需要返回位号集合，已移除
    public Profile add(Profile profile) {
        try {
            selectByNameAndType(profile.getName(), profile.getType(), profile.getTenantId());
            throw new DuplicateException("The profile already exists");
        } catch (NotFoundException notFoundException1) {
            if (profileMapper.insert(profile) > 0) {
                return profileMapper.selectById(profile.getId());
            }
            throw new ServiceException("The profile add failed");
        }
    }


    @Override
    // 2022-07-30 检查：通过
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
    // 2022-07-30 检查：不通过，修改模板是没有添加位号的，为啥还需要返回位号集合，已移除
    public Profile update(Profile profile) {
        selectById(profile.getId());
        profile.setUpdateTime(null);
        if (profileMapper.updateById(profile) > 0) {
            return profileMapper.selectById(profile.getId());
        }
        throw new ServiceException("The profile update failed");
    }

    @Override
    // 2022-07-30 检查：不通过，在查询模板的时候请勿全量返回改模板下的位号，因为位号可能会很多，已移除
    public Profile selectById(String id) {
        Profile profile = profileMapper.selectById(id);
        if (null == profile) {
            throw new NotFoundException("The profile does not exist");
        }
        return profile;
    }

    @Override
    // 2022-07-30 检查：不通过，在查询模板的时候请勿全量返回改模板下的位号，因为位号可能会很多，已移除
    public Profile selectByNameAndType(String name, Short type, String tenantId) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        queryWrapper.eq(Profile::getName, name);
        queryWrapper.eq(Profile::getType, type);
        queryWrapper.eq(Profile::getTenantId, tenantId);
        Profile profile = profileMapper.selectOne(queryWrapper);
        if (null == profile) {
            throw new NotFoundException("The profile does not exist");
        }
        return profile;
    }

    @Override
    // 2022-07-30 检查：不通过，在查询模板的时候请勿全量返回改模板下的位号，因为位号可能会很多，已移除
    public List<Profile> selectByIds(Set<String> ids) {
        List<Profile> profiles = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(ids)) {
            profiles = profileMapper.selectBatchIds(ids);
        }
        return profiles;
    }

    @Override
    // 2022-07-30 检查：不通过，在查询模板的时候请勿全量返回改模板下的位号，因为位号可能会很多，已移除
    public List<Profile> selectByDeviceId(String deviceId) {
        Set<String> profileIds = profileBindService.selectProfileIdsByDeviceId(deviceId);
        if (CollectionUtil.isNotEmpty(profileIds)) {
            return selectByIds(profileIds);
        }
        return new ArrayList<>();
    }

    @Override
    // 2022-07-30 检查：不通过，在查询模板的时候请勿全量返回改模板下的位号，因为位号可能会很多，已移除
    public Page<Profile> list(ProfileDto profileDto) {
        if (!Optional.ofNullable(profileDto.getPage()).isPresent()) {
            profileDto.setPage(new Pages());
        }
        return profileMapper.selectPage(profileDto.getPage().convert(), fuzzyQuery(profileDto));
    }

    @Override
    // 2022-07-30 检查：通过
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
