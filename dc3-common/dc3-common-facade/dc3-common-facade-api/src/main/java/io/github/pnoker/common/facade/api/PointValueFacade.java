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

import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;

import java.util.List;

/**
 * Protocol-neutral point value facade.
 * <p>
 * Mirrors the value-query RPCs on {@code api.center.data.PointValueApi} but returns plain
 * BO types so callers never have to touch gRPC or protobuf classes. Two implementations
 * back this interface:
 * <ul>
 * <li>{@code PointValueLocalFacade} — in-process call into {@code PointValueService},
 * selected when {@code dc3.facade.mode=local} (single deployment).</li>
 * <li>{@code PointValueGrpcFacade} — gRPC call against Data Center, selected when
 * {@code dc3.facade.mode=grpc} (distributed deployment, default).</li>
 * </ul>
 *
 * @author pnoker
 * @since 2026.5.5
 */
public interface PointValueFacade {

    /**
     * Query the latest collected value of a device point.
     *
     * @return the point value, or {@code null} when no value exists.
     */
    FacadePointValueBO lastValue(Long tenantId, Long deviceId, Long pointId);

    /**
     * Query historical values of a device point.
     *
     * @return an immutable list of value strings (never {@code null}; empty when nothing
     * matches).
     */
    List<String> history(Long tenantId, Long deviceId, Long pointId, int count);

}
