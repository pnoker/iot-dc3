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

package io.github.pnoker.common.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.query.TenantMembershipQuery;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.tenant.TenantContextHolder;

import java.util.List;
import java.util.Objects;

/**
 * Business service for tenant memberships.
 *
 * @author pnoker
 * @since 2026.6.12
 */
public interface TenantMembershipService {

    /**
     * Add a principal to a tenant with the membership attributes carried by the business object.
     *
     * @param membership tenant membership to create
     */
    void add(TenantMembershipBO membership);

    /**
     * Delete a tenant membership by its identifier.
     *
     * @param id membership identifier
     */
    void delete(Long id);

    /**
     * Find the membership that binds a principal to a tenant.
     *
     * @param tenantId    tenant identifier
     * @param principalId principal identifier
     * @return the matching membership, or {@code null} when it does not exist
     */
    TenantMembershipBO getByTenantIdAndPrincipalId(Long tenantId, Long principalId);

    /**
     * List the principals that belong to a tenant.
     *
     * @param tenantId tenant identifier
     * @return identifiers of principals with memberships in the tenant
     */
    List<Long> listPrincipalIdsByTenantId(Long tenantId);

    /**
     * Page through memberships that satisfy the supplied filters.
     *
     * @param entityQuery membership filters and pagination settings
     * @return the matching memberships
     */
    Page<TenantMembershipBO> list(TenantMembershipQuery entityQuery);

    /**
     * Get a tenant membership by its identifier.
     *
     * @param id membership identifier
     * @return the matching membership
     */
    TenantMembershipBO getById(Long id);

    /**
     * Determine whether a principal belongs to a tenant.
     *
     * @param tenantId    tenant identifier
     * @param principalId principal identifier
     * @return {@code true} when the membership exists; otherwise {@code false}
     */
    default boolean isTenantMember(Long tenantId, Long principalId) {
        // Membership is scoped by the explicit tenantId argument, so the tenant-line
        // interceptor's thread-local injection is redundant — and fatal before login,
        // when no tenant is bound on the thread (token/generate, check, logout, OAuth).
        return Objects.nonNull(TenantContextHolder.runIgnore(() -> getByTenantIdAndPrincipalId(tenantId, principalId)));
    }

    /**
     * Require a principal to belong to a tenant.
     *
     * @param tenantId    tenant identifier
     * @param principalId principal identifier
     * @throws NotFoundException when the membership does not exist
     */
    default void requireTenantMember(Long tenantId, Long principalId) {
        if (!isTenantMember(tenantId, principalId)) {
            throw new NotFoundException("Resource does not exist");
        }
    }

}
