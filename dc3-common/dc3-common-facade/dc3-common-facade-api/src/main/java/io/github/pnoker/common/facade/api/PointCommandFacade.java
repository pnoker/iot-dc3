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

/**
 * Protocol-neutral point command facade.
 * <p>
 * Mirrors the command RPCs on {@code api.center.data.PointValueApi} but returns plain
 * types so callers never have to touch gRPC or protobuf classes. Two implementations back
 * this interface:
 * <ul>
 * <li>{@code PointCommandLocalFacade} — in-process call into
 * {@code PointCommandService}, selected when {@code dc3.facade.mode=local} (single
 * deployment).</li>
 * <li>{@code PointCommandGrpcFacade} — gRPC call against Data Center, selected when
 * {@code dc3.facade.mode=grpc} (distributed deployment, default).</li>
 * </ul>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface PointCommandFacade {

    /**
     * Dispatch a read command to a device for a specific point.
     *
     * @param tenantId current tenant id
     * @param deviceId device id
     * @param pointId  point id
     * @return {@code true} if the command was accepted.
     */
    boolean submitRead(Long tenantId, Long deviceId, Long pointId);

    /**
     * Dispatch a write command to a device for a specific point.
     *
     * @param tenantId current tenant id
     * @param deviceId device id
     * @param pointId  point id
     * @param value    value to write
     * @return {@code true} if the command was accepted.
     */
    boolean submitWrite(Long tenantId, Long deviceId, Long pointId, String value);

}
