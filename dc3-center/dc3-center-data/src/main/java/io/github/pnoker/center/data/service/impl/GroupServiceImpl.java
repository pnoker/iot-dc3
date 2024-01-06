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

package io.github.pnoker.center.data.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.data.dal.GroupManager;
import io.github.pnoker.center.data.entity.builder.GroupForDataBuilder;
import io.github.pnoker.center.data.entity.model.GroupDO;
import io.github.pnoker.center.data.entity.query.GroupQuery;
import io.github.pnoker.center.data.service.GroupService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.bo.GroupBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
    private GroupForDataBuilder groupForDataBuilder;

    @Resource
    private GroupManager groupManager;

    @Override
    public void save(GroupBO entityBO) {
        checkDuplicate(entityBO, false, true);

        GroupDO entityDO = groupForDataBuilder.buildDOByBO(entityBO);
        if (!groupManager.save(entityDO)) {
            throw new AddException("分组创建失败");
        }
    }


    @Override
    public void remove(Long id) {
        getDOById(id, true);

        // 删除分组之前需要检查该分组是否存在关联
        LambdaQueryChainWrapper<GroupDO> wrapper = groupManager.lambdaQuery().eq(GroupDO::getParentGroupId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("分组删除失败，该分组下存在子分组");
        }

        if (!groupManager.removeById(id)) {
            throw new DeleteException("分组删除失败");
        }
    }


    @Override
    public void update(GroupBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        GroupDO entityDO = groupForDataBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!groupManager.updateById(entityDO)) {
            throw new UpdateException("分组更新失败");
        }
    }


    @Override
    public GroupBO selectById(Long id) {
        GroupDO entityDO = getDOById(id, true);
        return groupForDataBuilder.buildBOByDO(entityDO);
    }


    @Override
    public Page<GroupBO> selectByPage(GroupQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<GroupDO> entityPageDO = groupManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return groupForDataBuilder.buildBOPageByDOPage(entityPageDO);
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
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("分组重复");
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
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("分组不存在");
        }
        return entityDO;
    }

}
