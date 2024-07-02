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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.TenantBindManager;
import io.github.pnoker.common.auth.entity.bo.TenantBindBO;
import io.github.pnoker.common.auth.entity.builder.TenantBindBuilder;
import io.github.pnoker.common.auth.entity.model.TenantBindDO;
import io.github.pnoker.common.auth.entity.query.TenantBindQuery;
import io.github.pnoker.common.auth.service.TenantBindService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * TenantBindService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class TenantBindServiceImpl implements TenantBindService {

    @Resource
    private TenantBindBuilder tenantBindBuilder;

    @Resource
    private TenantBindManager tenantBindManager;

    @Override
    public void save(TenantBindBO entityBO) {
        checkDuplicate(entityBO, false, true);

        TenantBindDO entityDO = tenantBindBuilder.buildDOByBO(entityBO);
        if (!tenantBindManager.save(entityDO)) {
            throw new AddException("Failed to create tenant bind");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!tenantBindManager.removeById(id)) {
            throw new DeleteException("Failed to remove tenant bind");
        }
    }

    @Override
    public void update(TenantBindBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        TenantBindDO entityDO = tenantBindBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!tenantBindManager.updateById(entityDO)) {
            throw new UpdateException("The tenant bind update failed");
        }
    }

    @Override
    public TenantBindBO selectById(Long id) {
        TenantBindDO entityDO = getDOById(id, true);
        return tenantBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public TenantBindBO selectByTenantIdAndUserId(Long tenantId, Long userId) {
        LambdaQueryWrapper<TenantBindDO> wrapper = Wrappers.<TenantBindDO>query().lambda();
        wrapper.eq(TenantBindDO::getTenantId, tenantId);
        wrapper.eq(TenantBindDO::getUserId, userId);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        TenantBindDO entityDO = tenantBindManager.getOne(wrapper);
        return tenantBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<TenantBindBO> selectByPage(TenantBindQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<TenantBindDO> entityPageDO = tenantBindManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return tenantBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link TenantBindQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<TenantBindDO> fuzzyQuery(TenantBindQuery entityQuery) {
        LambdaQueryWrapper<TenantBindDO> wrapper = Wrappers.<TenantBindDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getTenantId()), TenantBindDO::getTenantId, entityQuery.getTenantId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getUserId()), TenantBindDO::getUserId, entityQuery.getUserId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link TenantBindBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(TenantBindBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<TenantBindDO> wrapper = Wrappers.<TenantBindDO>query().lambda();
        wrapper.eq(TenantBindDO::getTenantId, entityBO.getTenantId());
        wrapper.eq(TenantBindDO::getUserId, entityBO.getUserId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        TenantBindDO one = tenantBindManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Tenant has been duplicated");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link TenantBindDO}
     */
    private TenantBindDO getDOById(Long id, boolean throwException) {
        TenantBindDO entityDO = tenantBindManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Tenant bind does not exist");
        }
        return entityDO;
    }
}
