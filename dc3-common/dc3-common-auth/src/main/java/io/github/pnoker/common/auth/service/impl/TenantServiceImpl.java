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
import io.github.pnoker.common.auth.dal.TenantManager;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.builder.TenantBuilder;
import io.github.pnoker.common.auth.entity.model.TenantDO;
import io.github.pnoker.common.auth.entity.query.TenantQuery;
import io.github.pnoker.common.auth.service.TenantService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Business service implementation for tenant operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantBuilder tenantBuilder;

    private final TenantManager tenantManager;

    @Override
    public void add(TenantBO entityBO) {
        checkDuplicate(entityBO, false, true);

        TenantDO entityDO = tenantBuilder.buildDOByBO(entityBO);
        if (!tenantManager.save(entityDO)) {
            throw new AddException("Failed to create tenant: {}", entityBO.getTenantName());
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!tenantManager.removeById(id)) {
            throw new DeleteException("Failed to remove tenant");
        }
    }

    @Override
    public void update(TenantBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        TenantDO entityDO = tenantBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!tenantManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update tenant");
        }
    }

    @Override
    public TenantBO getById(Long id) {
        TenantDO entityDO = getDOById(id, true);
        return tenantBuilder.buildBOByDO(entityDO);
    }

    @Override
    public TenantBO getByCode(String code) {
        LambdaQueryWrapper<TenantDO> wrapper = Wrappers.<TenantDO>query().lambda();
        wrapper.eq(TenantDO::getTenantCode, code);
        wrapper.eq(TenantDO::getEnableFlag, EnableFlagEnum.ENABLE);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        TenantDO entityDO = tenantManager.getOne(wrapper);
        return tenantBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<TenantBO> list(TenantQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<TenantDO> entityPageDO = tenantManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return tenantBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * @param entityQuery {@link TenantQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<TenantDO> fuzzyQuery(TenantQuery entityQuery) {
        LambdaQueryWrapper<TenantDO> wrapper = Wrappers.<TenantDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getTenantName()), TenantDO::getTenantName,
                entityQuery.getTenantName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getTenantCode()), TenantDO::getTenantCode,
                entityQuery.getTenantCode());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), TenantDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        return wrapper;
    }

    /**
     * @param entityBO       {@link TenantBO}
     * @param isUpdate
     * @param throwException
     * @return
     */
    private boolean checkDuplicate(TenantBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<TenantDO> wrapper = Wrappers.<TenantDO>query().lambda();
        wrapper.eq(TenantDO::getTenantName, entityBO.getTenantName());
        wrapper.eq(TenantDO::getTenantCode, entityBO.getTenantCode());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        TenantDO one = tenantManager.getOne(wrapper);
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
     * @return {@link TenantDO}
     */
    private TenantDO getDOById(Long id, boolean throwException) {
        TenantDO entityDO = tenantManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Tenant");
        }
        return entityDO;
    }

}
