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
import io.github.pnoker.center.manager.entity.query.GroupPageQuery;
import io.github.pnoker.center.manager.mapper.GroupMapper;
import io.github.pnoker.center.manager.service.GroupService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.Group;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * GroupService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    @Resource
    private GroupMapper groupMapper;


    /**
     * {@inheritDoc}
     */
    @Override
    public void add(Group entityDO) {
        try {
            selectByName(entityDO.getGroupName(), entityDO.getTenantId());
            throw new DuplicateException("The device group already exists");
        } catch (NotFoundException notFoundException) {
            if (groupMapper.insert(entityDO) < 1) {
                throw new AddException("The group {} add failed", entityDO.getGroupName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String id) {
        Group group = selectById(id);
        if (ObjectUtil.isNull(group)) {
            throw new NotFoundException("The group does not exist");
        }

        if (groupMapper.deleteById(id) < 1) {
            throw new DeleteException("The group delete failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Group entityDO) {
        selectById(entityDO.getId());
        entityDO.setOperateTime(null);
        if (groupMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The group update failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Group selectById(String id) {
        Group group = groupMapper.selectById(id);
        if (ObjectUtil.isNull(group)) {
            throw new NotFoundException();
        }
        return group;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Group selectByName(String name, String tenantId) {
        LambdaQueryWrapper<Group> queryWrapper = Wrappers.<Group>query().lambda();
        queryWrapper.eq(Group::getGroupName, name);
        queryWrapper.last("limit 1");
        Group group = groupMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(group)) {
            throw new NotFoundException();
        }
        return group;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Group> list(GroupPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return groupMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    private LambdaQueryWrapper<Group> fuzzyQuery(GroupPageQuery query) {
        LambdaQueryWrapper<Group> queryWrapper = Wrappers.<Group>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getGroupName()), Group::getGroupName, query.getGroupName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getTenantId()), Group::getTenantId, query.getTenantId());
        }
        return queryWrapper;
    }

}
