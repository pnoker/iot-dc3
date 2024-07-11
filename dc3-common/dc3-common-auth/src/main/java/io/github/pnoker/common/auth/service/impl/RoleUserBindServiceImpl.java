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

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.RoleManager;
import io.github.pnoker.common.auth.dal.RoleUserBindManager;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleUserBindBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleUserBindBuilder;
import io.github.pnoker.common.auth.entity.model.RoleDO;
import io.github.pnoker.common.auth.entity.model.RoleUserBindDO;
import io.github.pnoker.common.auth.entity.query.RoleUserBindQuery;
import io.github.pnoker.common.auth.service.RoleUserBindService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author linys
 * @since 2022.1.0
 */
@Slf4j
@Service
public class RoleUserBindServiceImpl implements RoleUserBindService {

    @Resource
    private RoleUserBindBuilder roleUserBindBuilder;
    @Resource
    private RoleBuilder roleBuilder;

    @Resource
    private RoleUserBindManager roleUserBindManager;

    @Resource
    private RoleManager roleManager;


    @Override
    public void save(RoleUserBindBO entityBO) {
        checkDuplicate(entityBO, false, true);

        RoleUserBindDO entityDO = roleUserBindBuilder.buildDOByBO(entityBO);
        if (!roleUserBindManager.save(entityDO)) {
            throw new AddException("Failed to create role user bind");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!roleUserBindManager.removeById(id)) {
            throw new DeleteException("Failed to remove role user bind");
        }
    }

    @Override
    public void update(RoleUserBindBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        RoleUserBindDO entityDO = roleUserBindBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!roleUserBindManager.updateById(entityDO)) {
            throw new UpdateException("The role user bind update failed");
        }
    }

    @Override
    public RoleUserBindBO selectById(Long id) {
        RoleUserBindDO entityDO = getDOById(id, true);
        return roleUserBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<RoleUserBindBO> selectByPage(RoleUserBindQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RoleUserBindDO> entityPageDO = roleUserBindManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return roleUserBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public List<RoleBO> listRoleByTenantIdAndUserId(Long tenantId, Long userId) {
        LambdaQueryWrapper<RoleUserBindDO> wrapper = Wrappers.<RoleUserBindDO>query().lambda();
        wrapper.eq(RoleUserBindDO::getUserId, userId);
        List<RoleUserBindDO> roleUserBindBOList = roleUserBindManager.list(wrapper);
        if (CollUtil.isNotEmpty(roleUserBindBOList)) {
            List<RoleDO> roleBOList = roleManager.listByIds(roleUserBindBOList.stream().map(RoleUserBindDO::getRoleId)
                    .toList());
            List<RoleDO> collect = roleBOList.stream().filter(e -> EnableFlagEnum.ENABLE.getIndex().equals(e.getEnableFlag()) && tenantId.equals(e.getTenantId()))
                    .toList();
            return roleBuilder.buildBOListByDOList(collect);
        }

        return null;
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link RoleUserBindQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<RoleUserBindDO> fuzzyQuery(RoleUserBindQuery entityQuery) {
        LambdaQueryWrapper<RoleUserBindDO> wrapper = Wrappers.<RoleUserBindDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getUserId()), RoleUserBindDO::getUserId, entityQuery.getUserId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getRoleId()), RoleUserBindDO::getRoleId, entityQuery.getRoleId());
        wrapper.eq(RoleUserBindDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link RoleUserBindBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(RoleUserBindBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<RoleUserBindDO> wrapper = Wrappers.<RoleUserBindDO>query().lambda();
        wrapper.eq(RoleUserBindDO::getRoleId, entityBO.getRoleId());
        wrapper.eq(RoleUserBindDO::getUserId, entityBO.getUserId());
        wrapper.eq(RoleUserBindDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        RoleUserBindDO one = roleUserBindManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Role user bind has been duplicated");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link RoleUserBindDO}
     */
    private RoleUserBindDO getDOById(Long id, boolean throwException) {
        RoleUserBindDO entityDO = roleUserBindManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Role user bind does not exist");
        }
        return entityDO;
    }
}
