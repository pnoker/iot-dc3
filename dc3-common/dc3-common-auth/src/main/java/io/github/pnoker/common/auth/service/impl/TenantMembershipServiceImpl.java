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
import io.github.pnoker.common.auth.dal.TenantMembershipManager;
import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.builder.TenantMembershipBuilder;
import io.github.pnoker.common.auth.entity.model.TenantMembershipDO;
import io.github.pnoker.common.auth.entity.query.TenantMembershipQuery;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * Tenant membership service implementation.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Service
@RequiredArgsConstructor
public class TenantMembershipServiceImpl implements TenantMembershipService {

    private final TenantMembershipManager tenantMembershipManager;

    private final TenantMembershipBuilder tenantMembershipBuilder;

    @Override
    public void add(TenantMembershipBO membership) {
        TenantMembershipDO entityDO = tenantMembershipBuilder.buildDOByBO(membership);
        if (!tenantMembershipManager.save(entityDO)) {
            throw new AddException("Failed to create tenant membership");
        }
    }

    @Override
    public void delete(Long id) {
        if (Objects.isNull(tenantMembershipManager.getById(id))) {
            throw new NotFoundException("Tenant membership does not exist");
        }
        if (!tenantMembershipManager.removeById(id)) {
            throw new DeleteException("Failed to remove tenant membership");
        }
    }

    @Override
    public TenantMembershipBO getByTenantIdAndPrincipalId(Long tenantId, Long principalId) {
        if (Objects.isNull(tenantId) || Objects.isNull(principalId)) {
            return null;
        }
        LambdaQueryWrapper<TenantMembershipDO> wrapper = Wrappers.<TenantMembershipDO>query().lambda();
        wrapper.eq(TenantMembershipDO::getTenantId, tenantId);
        wrapper.eq(TenantMembershipDO::getPrincipalId, principalId);
        wrapper.eq(TenantMembershipDO::getMembershipStatus, "ACTIVE");
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        TenantMembershipDO entityDO = tenantMembershipManager.getOne(wrapper);
        return Objects.isNull(entityDO) ? null : tenantMembershipBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<Long> listPrincipalIdsByTenantId(Long tenantId) {
        if (Objects.isNull(tenantId)) {
            return List.of();
        }
        LambdaQueryWrapper<TenantMembershipDO> wrapper = Wrappers.<TenantMembershipDO>query().lambda();
        wrapper.eq(TenantMembershipDO::getTenantId, tenantId);
        wrapper.eq(TenantMembershipDO::getMembershipStatus, "ACTIVE");
        wrapper.select(TenantMembershipDO::getPrincipalId);
        List<Long> principalIds = tenantMembershipManager.listObjs(wrapper, o -> (Long) o);
        if (CollectionUtils.isEmpty(principalIds)) {
            return List.of();
        }
        return principalIds;
    }

    @Override
    public TenantMembershipBO getById(Long id) {
        TenantMembershipDO entityDO = tenantMembershipManager.getById(id);
        if (Objects.isNull(entityDO)) {
            throw new NotFoundException("Tenant membership does not exist");
        }
        return tenantMembershipBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<TenantMembershipBO> list(TenantMembershipQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<TenantMembershipDO> page = tenantMembershipManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return tenantMembershipBuilder.buildBOPageByDOPage(page);
    }

    /**
     * Build fuzzy query wrapper for tenant membership search.
     *
     * @param entityQuery {@link TenantMembershipQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link TenantMembershipDO}
     */
    private LambdaQueryWrapper<TenantMembershipDO> fuzzyQuery(TenantMembershipQuery entityQuery) {
        LambdaQueryWrapper<TenantMembershipDO> wrapper = Wrappers.<TenantMembershipDO>query().lambda();
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), TenantMembershipDO::getTenantId,
                entityQuery.getTenantId());
        wrapper.eq(Objects.nonNull(entityQuery.getPrincipalId()), TenantMembershipDO::getPrincipalId,
                entityQuery.getPrincipalId());
        wrapper.eq(Objects.nonNull(entityQuery.getPrincipalType()), TenantMembershipDO::getPrincipalType,
                Objects.isNull(entityQuery.getPrincipalType()) ? null : entityQuery.getPrincipalType().getValue());
        wrapper.eq(Objects.nonNull(entityQuery.getMembershipStatus()), TenantMembershipDO::getMembershipStatus,
                Objects.isNull(entityQuery.getMembershipStatus()) ? null
                        : entityQuery.getMembershipStatus().getValue());
        return wrapper;
    }

}
