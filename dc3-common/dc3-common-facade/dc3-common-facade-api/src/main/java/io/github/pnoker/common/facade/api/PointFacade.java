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

package io.github.pnoker.common.facade.api;

import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Protocol-neutral point facade. Mirrors the two RPCs on
 * {@code api.center.manager.PointApi}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface PointFacade {

    private static boolean matchesTenant(Long tenantId, FacadePointBO point) {
        return Objects.nonNull(point) && Objects.equals(tenantId, point.getTenantId());
    }

    /**
     * @return the point, or {@code null} when it does not exist.
     */
    FacadePointBO selectById(Long id);

    /**
     * Tenant-scoped single lookup. Returns {@code null} when the point is missing or
     * belongs to another tenant.
     */
    default FacadePointBO selectById(Long tenantId, Long id) {
        if (Objects.isNull(tenantId)) {
            return null;
        }
        FacadePointBO point = selectById(id);
        return matchesTenant(tenantId, point) ? point : null;
    }

    /**
     * Bulk lookup. Avoids the N+1 cost of calling {@link #selectById(Long)} in a loop.
     *
     * @return list of resolved points (missing ids are simply omitted; never {@code null}).
     */
    List<FacadePointBO> selectByIds(Collection<Long> ids);

    /**
     * Tenant-scoped bulk lookup. Missing or cross-tenant points are omitted.
     */
    default List<FacadePointBO> selectByIds(Long tenantId, Collection<Long> ids) {
        if (Objects.isNull(tenantId) || Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectByIds(ids).stream()
                .filter(point -> matchesTenant(tenantId, point))
                .toList();
    }

    /**
     * @return a page of points (never {@code null}; empty page when nothing matches).
     */
    FacadePage<FacadePointBO> selectByPage(FacadePointQuery query);

}
