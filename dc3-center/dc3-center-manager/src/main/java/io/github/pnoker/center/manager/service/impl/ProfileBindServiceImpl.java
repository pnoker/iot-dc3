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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.mapper.ProfileBindMapper;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.common.bean.Pages;
import io.github.pnoker.common.dto.ProfileBindDto;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.ProfileBind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
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
    private ProfileBindMapper profileBindMapper;

    @Override
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
    public boolean delete(String id) {
        selectById(id);
        return profileBindMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteByDeviceId(String deviceId) {
        ProfileBindDto profileBindDto = new ProfileBindDto();
        profileBindDto.setDeviceId(deviceId);
        return profileBindMapper.delete(fuzzyQuery(profileBindDto)) > 0;
    }

    @Override
    public boolean deleteByDeviceIdAndProfileId(String deviceId, String profileId) {
        ProfileBindDto profileBindDto = new ProfileBindDto();
        profileBindDto.setProfileId(profileId);
        profileBindDto.setDeviceId(deviceId);
        return profileBindMapper.delete(fuzzyQuery(profileBindDto)) > 0;
    }

    @Override
    public ProfileBind update(ProfileBind profileBind) {
        selectById(profileBind.getId());
        profileBind.setUpdateTime(null);
        if (profileBindMapper.updateById(profileBind) > 0) {
            return profileBindMapper.selectById(profileBind.getId());
        }
        throw new ServiceException("The profile bind update failed");
    }

    @Override
    public ProfileBind selectById(String id) {
        ProfileBind profileBind = profileBindMapper.selectById(id);
        if (null == profileBind) {
            throw new NotFoundException("The profile bind does not exist");
        }
        return profileBind;
    }

    @Override
    public ProfileBind selectByDeviceIdAndProfileId(String deviceId, String profileId) {
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
    public Set<String> selectDeviceIdsByProfileId(String profileId) {
        ProfileBindDto profileBindDto = new ProfileBindDto();
        profileBindDto.setProfileId(profileId);
        List<ProfileBind> profileBinds = profileBindMapper.selectList(fuzzyQuery(profileBindDto));
        return profileBinds.stream().map(ProfileBind::getDeviceId).collect(Collectors.toSet());
    }

    @Override
    public Set<String> selectProfileIdsByDeviceId(String deviceId) {
        ProfileBindDto profileBindDto = new ProfileBindDto();
        profileBindDto.setDeviceId(deviceId);
        List<ProfileBind> profileBinds = profileBindMapper.selectList(fuzzyQuery(profileBindDto));
        return profileBinds.stream().map(ProfileBind::getProfileId).collect(Collectors.toSet());
    }

    @Override
    public Page<ProfileBind> list(ProfileBindDto profileBindDto) {
        if (ObjectUtil.isNull(profileBindDto.getPage())) {
            profileBindDto.setPage(new Pages());
        }
        return profileBindMapper.selectPage(profileBindDto.getPage().convert(), fuzzyQuery(profileBindDto));
    }

    @Override
    public LambdaQueryWrapper<ProfileBind> fuzzyQuery(ProfileBindDto profileBindDto) {
        LambdaQueryWrapper<ProfileBind> queryWrapper = Wrappers.<ProfileBind>query().lambda();
        if (ObjectUtil.isNotNull(profileBindDto)) {
            queryWrapper.eq(StrUtil.isNotEmpty(profileBindDto.getProfileId()), ProfileBind::getProfileId, profileBindDto.getProfileId());
            queryWrapper.eq(StrUtil.isNotEmpty(profileBindDto.getDeviceId()), ProfileBind::getDeviceId, profileBindDto.getDeviceId());
        }
        return queryWrapper;
    }

}
