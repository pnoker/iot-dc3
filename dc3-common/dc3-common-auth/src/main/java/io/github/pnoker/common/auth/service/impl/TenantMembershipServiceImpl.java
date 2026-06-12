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
import io.github.pnoker.common.auth.dal.TenantMembershipManager;
import io.github.pnoker.common.auth.entity.model.TenantMembershipDO;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.NotFoundException;
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

    @Override
    public void add(TenantMembershipDO membership) {
        if (!tenantMembershipManager.save(membership)) {
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
    public TenantMembershipDO getByTenantIdAndPrincipalId(Long tenantId, Long principalId) {
        if (Objects.isNull(tenantId) || Objects.isNull(principalId)) {
            return null;
        }
        LambdaQueryWrapper<TenantMembershipDO> wrapper = Wrappers.<TenantMembershipDO>query().lambda();
        wrapper.eq(TenantMembershipDO::getTenantId, tenantId);
        wrapper.eq(TenantMembershipDO::getPrincipalId, principalId);
        wrapper.eq(TenantMembershipDO::getMembershipStatus, "ACTIVE");
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        return tenantMembershipManager.getOne(wrapper);
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

}
