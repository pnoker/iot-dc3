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

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.ProfileBindBOPageQuery;
import io.github.pnoker.center.manager.mapper.ProfileBindMapper;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.center.manager.entity.bo.ProfileBindBO;
import io.github.pnoker.common.utils.PageUtil;
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
    public void save(ProfileBindBO entityBO) {
        try {
            selectByDeviceIdAndProfileId(entityBO.getDeviceId(), entityBO.getProfileId());
            throw new DuplicateException("The profile bind already exists");
        } catch (NotFoundException notFoundException) {
            if (profileBindMapper.insert(entityBO) < 1) {
                throw new AddException("The profile bind add failed");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Long id) {
        ProfileBindBO profileBindBO = selectById(id);
        if (ObjectUtil.isNull(profileBindBO)) {
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
    public Boolean deleteByDeviceId(Long deviceId) {
        ProfileBindBOPageQuery profileBindPageQuery = new ProfileBindBOPageQuery();
        profileBindPageQuery.setDeviceId(deviceId);
        return profileBindMapper.delete(fuzzyQuery(profileBindPageQuery)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean deleteByDeviceIdAndProfileId(Long deviceId, Long profileId) {
        ProfileBindBOPageQuery profileBindPageQuery = new ProfileBindBOPageQuery();
        profileBindPageQuery.setProfileId(profileId);
        profileBindPageQuery.setDeviceId(deviceId);
        return profileBindMapper.delete(fuzzyQuery(profileBindPageQuery)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(ProfileBindBO entityBO) {
        selectById(entityBO.getId());
        entityBO.setOperateTime(null);
        if (profileBindMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The profile bind update failed");
        }
    }

    @Override
    public ProfileBindBO selectById(Long id) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProfileBindBO selectByDeviceIdAndProfileId(Long deviceId, Long profileId) {
        ProfileBindBOPageQuery profileBindPageQuery = new ProfileBindBOPageQuery();
        profileBindPageQuery.setDeviceId(deviceId);
        profileBindPageQuery.setProfileId(profileId);
        LambdaQueryWrapper<ProfileBindBO> queryWrapper = fuzzyQuery(profileBindPageQuery);
        queryWrapper.last("limit 1");
        ProfileBindBO profileBindBO = profileBindMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(profileBindBO)) {
            throw new NotFoundException();
        }
        return profileBindBO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Long> selectDeviceIdsByProfileId(Long profileId) {
        ProfileBindBOPageQuery profileBindPageQuery = new ProfileBindBOPageQuery();
        profileBindPageQuery.setProfileId(profileId);
        List<ProfileBindBO> profileBindBOS = profileBindMapper.selectList(fuzzyQuery(profileBindPageQuery));
        return profileBindBOS.stream().map(ProfileBindBO::getDeviceId).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Long> selectProfileIdsByDeviceId(Long deviceId) {
        ProfileBindBOPageQuery profileBindPageQuery = new ProfileBindBOPageQuery();
        profileBindPageQuery.setDeviceId(deviceId);
        List<ProfileBindBO> profileBindBOS = profileBindMapper.selectList(fuzzyQuery(profileBindPageQuery));
        return profileBindBOS.stream().map(ProfileBindBO::getProfileId).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ProfileBindBO> selectByPage(ProfileBindBOPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return profileBindMapper.selectPage(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
    }

    private LambdaQueryWrapper<ProfileBindBO> fuzzyQuery(ProfileBindBOPageQuery query) {
        LambdaQueryWrapper<ProfileBindBO> queryWrapper = Wrappers.<ProfileBindBO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getProfileId()), ProfileBindBO::getProfileId, query.getProfileId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getDeviceId()), ProfileBindBO::getDeviceId, query.getDeviceId());
        }
        return queryWrapper;
    }

}
