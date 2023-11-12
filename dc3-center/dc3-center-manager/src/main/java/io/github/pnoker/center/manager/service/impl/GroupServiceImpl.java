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
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * <p>
 * GroupService Impl
 * </p>
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
    public void add(GroupDO entityBO) {
        Optional<GroupDO> groupDO = selectByName(entityBO.getGroupName(), entityBO.getTenantId());
        if (groupDO.isPresent()) {
            throw new DuplicateException("The group[name={}] already exists", entityBO.getGroupName());
        }
        if (!groupManager.save(entityBO)) {
            throw new AddException("The group[name={}] add failed", entityBO.getGroupName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Long id) {
        Optional<GroupDO> groupDO = selectById(id);
        if (!groupDO.isPresent()) {
            throw new NotFoundException("The group[id={}] does not exist", id);
        }
        if (!groupManager.removeById(id)) {
            throw new DeleteException("The group[id={}] delete failed", id);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(GroupDO entityBO) {
        Optional<GroupDO> groupDO = selectById(entityBO.getId());
        if (!groupDO.isPresent()) {
            throw new NotFoundException("The group[id={}] does not exist", entityBO.getId());
        }
        entityBO.setOperateTime(null);
        if (!groupManager.updateById(entityBO)) {
            throw new UpdateException("The group[id={}] update failed", entityBO.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<GroupDO> selectById(Long id) {
        return groupManager.getOptById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<GroupDO> selectByName(String name, Long tenantId) {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.<GroupDO>query().lambda();
        queryWrapper.eq(GroupDO::getGroupName, name);
        queryWrapper.last("limit 1");
        return groupManager.getOneOpt(queryWrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<GroupDO> list(GroupPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return groupManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
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
