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

import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Protocol-neutral driver facade.
 * <p>
 * Mirrors the three RPCs on {@code api.center.manager.DriverApi}. Implementation
 * selection follows the same {@code dc3.facade.mode} switch used by {@link DeviceFacade}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.5
 */
public interface DriverFacade {

    private static boolean matchesTenant(Long tenantId, FacadeDriverBO driver) {
        return Objects.nonNull(driver) && Objects.equals(tenantId, driver.getTenantId());
    }

    /**
     * @return the driver, or {@code null} when it does not exist.
     */
    FacadeDriverBO selectById(Long id);

    /**
     * Tenant-scoped single lookup. Returns {@code null} when the driver is missing or
     * belongs to another tenant.
     */
    default FacadeDriverBO selectById(Long tenantId, Long id) {
        if (Objects.isNull(tenantId)) {
            return null;
        }
        FacadeDriverBO driver = selectById(id);
        return matchesTenant(tenantId, driver) ? driver : null;
    }

    /**
     * Bulk lookup. Avoids the N+1 cost of calling {@link #selectById(Long)} in a loop.
     *
     * @return list of resolved drivers (missing ids are simply omitted; never {@code
     * null}).
     */
    List<FacadeDriverBO> selectByIds(Collection<Long> ids);

    /**
     * Tenant-scoped bulk lookup. Missing or cross-tenant drivers are omitted.
     */
    default List<FacadeDriverBO> selectByIds(Long tenantId, Collection<Long> ids) {
        if (Objects.isNull(tenantId) || Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectByIds(ids).stream()
                .filter(driver -> matchesTenant(tenantId, driver))
                .toList();
    }

    /**
     * @return a page of drivers (never {@code null}; empty page when nothing matches).
     */
    FacadePage<FacadeDriverBO> selectByPage(FacadeDriverQuery query);

    /**
     * Resolve the driver that owns a given device.
     *
     * @return the driver, or {@code null} when the device has no bound driver.
     */
    FacadeDriverBO selectByDeviceId(Long deviceId);

    /**
     * Tenant-scoped owner lookup. Returns {@code null} when the owning driver is missing
     * or belongs to another tenant.
     */
    default FacadeDriverBO selectByDeviceId(Long tenantId, Long deviceId) {
        if (Objects.isNull(tenantId)) {
            return null;
        }
        FacadeDriverBO driver = selectByDeviceId(deviceId);
        return matchesTenant(tenantId, driver) ? driver : null;
    }

}
