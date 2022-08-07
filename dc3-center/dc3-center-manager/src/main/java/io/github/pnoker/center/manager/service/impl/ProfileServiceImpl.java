/*
 * Copyright 2022 Pnoker All Rights Reserved
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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.mapper.ProfileMapper;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.center.manager.service.ProfileService;
import io.github.pnoker.common.bean.Pages;
import io.github.pnoker.common.dto.ProfileDto;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Profile;
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
        if (ObjectUtil.isNull(profileDto.getPage())) {
            profileDto.setPage(new Pages());
        }
        return profileMapper.selectPageWithDevice(profileDto.getPage().convert(), customFuzzyQuery(profileDto), profileDto.getDeviceId());
    }

    @Override
    // 2022-07-30 检查：通过
    public LambdaQueryWrapper<Profile> fuzzyQuery(ProfileDto profileDto) {
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        if (ObjectUtil.isNotEmpty(profileDto)) {
            queryWrapper.like(StrUtil.isNotBlank(profileDto.getName()), Profile::getName, profileDto.getName());
            queryWrapper.eq(ObjectUtil.isNotEmpty(profileDto.getShare()), Profile::getShare, profileDto.getShare());
            queryWrapper.eq(ObjectUtil.isNotEmpty(profileDto.getEnable()), Profile::getEnable, profileDto.getEnable());
            queryWrapper.eq(StrUtil.isNotBlank(profileDto.getTenantId()), Profile::getTenantId, profileDto.getTenantId());
        }
        return queryWrapper;
    }

    public LambdaQueryWrapper<Profile> customFuzzyQuery(ProfileDto profileDto) {
        QueryWrapper<Profile> queryWrapper = Wrappers.query();
        queryWrapper.eq("dp.deleted", 0);
        if (ObjectUtil.isNotNull(profileDto)) {
            queryWrapper.like(StrUtil.isNotEmpty(profileDto.getName()), "dp.name", profileDto.getName());
            queryWrapper.eq(ObjectUtil.isNotNull(profileDto.getShare()), "dp.share", profileDto.getShare());
            queryWrapper.eq(ObjectUtil.isNotNull(profileDto.getEnable()), "dp.enable", profileDto.getEnable());
            queryWrapper.eq(StrUtil.isNotEmpty(profileDto.getTenantId()), "dp.tenant_id", profileDto.getTenantId());
        }
        return queryWrapper.lambda();
    }

}
