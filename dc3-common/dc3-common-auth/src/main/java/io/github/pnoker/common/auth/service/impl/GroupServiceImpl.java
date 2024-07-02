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

package io.github.pnoker.common.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.GroupManager;
import io.github.pnoker.common.auth.entity.builder.GroupForAuthBuilder;
import io.github.pnoker.common.auth.entity.model.GroupDO;
import io.github.pnoker.common.auth.entity.query.GroupQuery;
import io.github.pnoker.common.auth.service.GroupService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.bo.GroupBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * Group Service Impl
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    @Resource
    private GroupForAuthBuilder groupForAuthBuilder;

    @Resource
    private GroupManager groupManager;

    @Override
    public void save(GroupBO entityBO) {
        checkDuplicate(entityBO, false, true);

        GroupDO entityDO = groupForAuthBuilder.buildDOByBO(entityBO);
        if (!groupManager.save(entityDO)) {
            throw new AddException("Failed to create group");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        // 删除分组之前需要检查该分组是否存在关联
        LambdaQueryChainWrapper<GroupDO> wrapper = groupManager.lambdaQuery().eq(GroupDO::getParentGroupId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove group: there are subgroups under the group");
        }

        if (!groupManager.removeById(id)) {
            throw new DeleteException("Failed to remove group");
        }
    }

    @Override
    public void update(GroupBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        GroupDO entityDO = groupForAuthBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!groupManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update group");
        }
    }

    @Override
    public GroupBO selectById(Long id) {
        GroupDO entityDO = getDOById(id, true);
        return groupForAuthBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<GroupBO> selectByPage(GroupQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<GroupDO> entityPageDO = groupManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return groupForAuthBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link GroupQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<GroupDO> fuzzyQuery(GroupQuery entityQuery) {
        LambdaQueryWrapper<GroupDO> wrapper = Wrappers.<GroupDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getGroupName()), GroupDO::getGroupName, entityQuery.getGroupName());
        wrapper.eq(GroupDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link GroupBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(GroupBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<GroupDO> wrapper = Wrappers.<GroupDO>query().lambda();
        wrapper.eq(GroupDO::getGroupName, entityBO.getGroupName());
        wrapper.eq(GroupDO::getGroupTypeFlag, entityBO.getGroupTypeFlag());
        wrapper.eq(GroupDO::getParentGroupId, entityBO.getParentGroupId());
        wrapper.eq(GroupDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        GroupDO one = groupManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Group has been duplicated");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link GroupDO}
     */
    private GroupDO getDOById(Long id, boolean throwException) {
        GroupDO entityDO = groupManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Group does not exist");
        }
        return entityDO;
    }

}
