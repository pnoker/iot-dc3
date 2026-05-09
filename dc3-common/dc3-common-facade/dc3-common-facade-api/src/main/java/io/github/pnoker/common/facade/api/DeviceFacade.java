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

import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Protocol-neutral device facade.
 * <p>
 * Mirrors the four RPCs on {@code api.center.manager.DeviceApi} but returns plain BO/Page
 * types so callers never have to touch gRPC or protobuf classes. Two implementations back
 * this interface:
 * <ul>
 * <li>{@code DeviceLocalFacade} — in-process call into {@code DeviceService}, selected
 * when {@code dc3.facade.mode=local} (single deployment).</li>
 * <li>{@code DeviceGrpcFacade} — gRPC call against Manager Center, selected when
 * {@code dc3.facade.mode=grpc} (distributed deployment, default).</li>
 * </ul>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.5
 */
public interface DeviceFacade {

    /**
     * Query a single device by id.
     *
     * @return the device, or {@code null} when the device does not exist.
     */
    FacadeDeviceBO selectById(Long id);

    /**
     * Tenant-scoped single lookup. Returns {@code null} when the device is missing or
     * belongs to another tenant.
     */
    default FacadeDeviceBO selectById(Long tenantId, Long id) {
        if (Objects.isNull(tenantId)) {
            return null;
        }
        FacadeDeviceBO device = selectById(id);
        return matchesTenant(tenantId, device) ? device : null;
    }

    /**
     * Bulk lookup. Avoids the N+1 cost of calling {@link #selectById(Long)} in a loop.
     *
     * @return list of resolved devices (missing ids are simply omitted; never {@code
     * null}).
     */
    List<FacadeDeviceBO> selectByIds(Collection<Long> ids);

    /**
     * Tenant-scoped bulk lookup. Missing or cross-tenant devices are omitted.
     */
    default List<FacadeDeviceBO> selectByIds(Long tenantId, Collection<Long> ids) {
        if (Objects.isNull(tenantId) || Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectByIds(ids).stream()
                .filter(device -> matchesTenant(tenantId, device))
                .toList();
    }

    /**
     * Paginated query.
     *
     * @return a page of devices (never {@code null}; empty page when nothing matches).
     */
    FacadePage<FacadeDeviceBO> selectByPage(FacadeDeviceQuery query);

    /**
     * List devices attached to a given profile.
     *
     * @return an immutable list (never {@code null}; empty when nothing matches).
     */
    List<FacadeDeviceBO> selectByProfileId(Long profileId);

    /**
     * Tenant-scoped lookup by profile. Cross-tenant devices are omitted.
     */
    default List<FacadeDeviceBO> selectByProfileId(Long tenantId, Long profileId) {
        if (Objects.isNull(tenantId)) {
            return Collections.emptyList();
        }
        return selectByProfileId(profileId).stream()
                .filter(device -> matchesTenant(tenantId, device))
                .toList();
    }

    /**
     * List devices attached to a given driver.
     *
     * @return an immutable list (never {@code null}; empty when nothing matches).
     */
    List<FacadeDeviceBO> selectByDriverId(Long driverId);

    /**
     * Tenant-scoped lookup by driver. Cross-tenant devices are omitted.
     */
    default List<FacadeDeviceBO> selectByDriverId(Long tenantId, Long driverId) {
        if (Objects.isNull(tenantId)) {
            return Collections.emptyList();
        }
        return selectByDriverId(driverId).stream()
                .filter(device -> matchesTenant(tenantId, device))
                .toList();
    }

    private static boolean matchesTenant(Long tenantId, FacadeDeviceBO device) {
        return Objects.nonNull(device) && Objects.equals(tenantId, device.getTenantId());
    }

}
