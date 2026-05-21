/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Business service implementation for tenant binding operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantBindServiceImpl implements TenantBindService {

    private final TenantBindBuilder tenantBindBuilder;

    private final TenantBindManager tenantBindManager;

    @Override
    public void add(TenantBindBO entityBO) {
        checkDuplicate(entityBO, false, true);

        TenantBindDO entityDO = tenantBindBuilder.buildDOByBO(entityBO);
        if (!tenantBindManager.save(entityDO)) {
            throw new AddException("Failed to create tenant bind");
        }
    }

    @Override
    public void delete(Long id) {
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
    public TenantBindBO getById(Long id) {
        TenantBindDO entityDO = getDOById(id, true);
        return tenantBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public TenantBindBO getByTenantIdAndUserId(Long tenantId, Long userId) {
        LambdaQueryWrapper<TenantBindDO> wrapper = Wrappers.<TenantBindDO>query().lambda();
        wrapper.eq(TenantBindDO::getTenantId, tenantId);
        wrapper.eq(TenantBindDO::getUserId, userId);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        TenantBindDO entityDO = tenantBindManager.getOne(wrapper);
        return tenantBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<Long> listUserIdsByTenantId(Long tenantId) {
        if (Objects.isNull(tenantId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TenantBindDO> wrapper = Wrappers.<TenantBindDO>query().lambda();
        wrapper.eq(TenantBindDO::getTenantId, tenantId);
        wrapper.select(TenantBindDO::getUserId);
        return tenantBindManager.listObjs(wrapper, o -> (Long) o);
    }

    @Override
    public Page<TenantBindBO> list(TenantBindQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<TenantBindDO> entityPageDO = tenantBindManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return tenantBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * @param entityQuery {@link TenantBindQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<TenantBindDO> fuzzyQuery(TenantBindQuery entityQuery) {
        LambdaQueryWrapper<TenantBindDO> wrapper = Wrappers.<TenantBindDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getTenantId()), TenantBindDO::getTenantId,
                entityQuery.getTenantId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getUserId()), TenantBindDO::getUserId, entityQuery.getUserId());
        return wrapper;
    }

    /**
     * @param entityBO       {@link TenantBindBO}
     * @param isUpdate
     * @param throwException
     * @return
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
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
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
