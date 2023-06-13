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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.ProfileBindPageQuery;
import io.github.pnoker.center.manager.mapper.ProfileBindMapper;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
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
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ProfileBindServiceImpl implements ProfileBindService {

    @Resource
    private ProfileBindMapper profileBindMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(ProfileBind entityDO) {
        try {
            selectByDeviceIdAndProfileId(entityDO.getDeviceId(), entityDO.getProfileId());
            throw new DuplicateException("The profile bind already exists");
        } catch (NotFoundException notFoundException) {
            if (profileBindMapper.insert(entityDO) < 1) {
                throw new AddException("The profile bind add failed");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String id) {
        ProfileBind profileBind = selectById(id);
        if (ObjectUtil.isNull(profileBind)) {
            throw new NotFoundException("The profile bind does not exist");
        }

        if (profileBindMapper.deleteById(id) < 1) {
            throw new DeleteException("The profile bind delete failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean deleteByDeviceId(String deviceId) {
        ProfileBindPageQuery profileBindPageQuery = new ProfileBindPageQuery();
        profileBindPageQuery.setDeviceId(deviceId);
        return profileBindMapper.delete(fuzzyQuery(profileBindPageQuery)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean deleteByDeviceIdAndProfileId(String deviceId, String profileId) {
        ProfileBindPageQuery profileBindPageQuery = new ProfileBindPageQuery();
        profileBindPageQuery.setProfileId(profileId);
        profileBindPageQuery.setDeviceId(deviceId);
        return profileBindMapper.delete(fuzzyQuery(profileBindPageQuery)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(ProfileBind entityDO) {
        selectById(entityDO.getId());
        entityDO.setOperateTime(null);
        if (profileBindMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The profile bind update failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProfileBind selectById(String id) {
        ProfileBind profileBind = profileBindMapper.selectById(id);
        if (ObjectUtil.isNull(profileBind)) {
            throw new NotFoundException();
        }
        return profileBind;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProfileBind selectByDeviceIdAndProfileId(String deviceId, String profileId) {
        ProfileBindPageQuery profileBindPageQuery = new ProfileBindPageQuery();
        profileBindPageQuery.setDeviceId(deviceId);
        profileBindPageQuery.setProfileId(profileId);
        LambdaQueryWrapper<ProfileBind> queryWrapper = fuzzyQuery(profileBindPageQuery);
        queryWrapper.last("limit 1");
        ProfileBind profileBind = profileBindMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(profileBind)) {
            throw new NotFoundException();
        }
        return profileBind;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> selectDeviceIdsByProfileId(String profileId) {
        ProfileBindPageQuery profileBindPageQuery = new ProfileBindPageQuery();
        profileBindPageQuery.setProfileId(profileId);
        List<ProfileBind> profileBinds = profileBindMapper.selectList(fuzzyQuery(profileBindPageQuery));
        return profileBinds.stream().map(ProfileBind::getDeviceId).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> selectProfileIdsByDeviceId(String deviceId) {
        ProfileBindPageQuery profileBindPageQuery = new ProfileBindPageQuery();
        profileBindPageQuery.setDeviceId(deviceId);
        List<ProfileBind> profileBinds = profileBindMapper.selectList(fuzzyQuery(profileBindPageQuery));
        return profileBinds.stream().map(ProfileBind::getProfileId).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ProfileBind> list(ProfileBindPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return profileBindMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    private LambdaQueryWrapper<ProfileBind> fuzzyQuery(ProfileBindPageQuery query) {
        LambdaQueryWrapper<ProfileBind> queryWrapper = Wrappers.<ProfileBind>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getProfileId()), ProfileBind::getProfileId, query.getProfileId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getDeviceId()), ProfileBind::getDeviceId, query.getDeviceId());
        }
        return queryWrapper;
    }

}
