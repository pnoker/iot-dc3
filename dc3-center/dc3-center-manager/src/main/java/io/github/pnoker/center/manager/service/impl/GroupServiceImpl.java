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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.model.GroupDO;
import io.github.pnoker.center.manager.entity.query.GroupPageQuery;
import io.github.pnoker.center.manager.manager.GroupManager;
import io.github.pnoker.center.manager.service.GroupService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
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
    private GroupManager groupManager;


    /**
     * {@inheritDoc}
     */
    @Override
    public void add(GroupDO entityDO) {
        try {
            selectByName(entityDO.getGroupName(), entityDO.getTenantId());
            throw new DuplicateException("The device group already exists");
        } catch (NotFoundException notFoundException) {
            if (!groupManager.save(entityDO)) {
                throw new AddException("The group {} add failed", entityDO.getGroupName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Long id) {
        GroupDO group = selectById(id);
        if (ObjectUtil.isNull(group)) {
            throw new NotFoundException("The group does not exist");
        }

        if (!groupManager.removeById(id)) {
            throw new DeleteException("The group delete failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(GroupDO entityDO) {
        selectById(entityDO.getId());
        entityDO.setOperateTime(null);
        if (!groupManager.updateById(entityDO)) {
            throw new UpdateException("The group update failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GroupDO selectById(Long id) {
        GroupDO group = groupManager.getById(id);
        if (ObjectUtil.isNull(group)) {
            throw new NotFoundException();
        }
        return group;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GroupDO selectByName(String name, Long tenantId) {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.<GroupDO>query().lambda();
        queryWrapper.eq(GroupDO::getGroupName, name);
        queryWrapper.last("limit 1");
        GroupDO group = groupManager.getOne(queryWrapper);
        if (ObjectUtil.isNull(group)) {
            throw new NotFoundException();
        }
        return group;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<GroupDO> list(GroupPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return groupManager.page(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    private LambdaQueryWrapper<GroupDO> fuzzyQuery(GroupPageQuery query) {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.<GroupDO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getGroupName()), GroupDO::getGroupName, query.getGroupName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getTenantId()), GroupDO::getTenantId, query.getTenantId());
        }
        return queryWrapper;
    }

}
