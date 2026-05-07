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

import java.util.List;

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
     * List devices attached to a given driver.
     *
     * @return an immutable list (never {@code null}; empty when nothing matches).
     */
    List<FacadeDeviceBO> selectByDriverId(Long driverId);

}
