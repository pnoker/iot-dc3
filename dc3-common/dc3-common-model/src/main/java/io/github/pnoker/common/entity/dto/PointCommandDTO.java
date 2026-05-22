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

package io.github.pnoker.common.entity.dto;

import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.github.pnoker.common.enums.PointCommandTypeEnum;

import java.time.Instant;

/**
 * Data transfer object for point command dispatch via RabbitMQ.
 * <p>
 * Replaces the old {@code content} JSON-string pattern with a strongly-typed,
 * polymorphic {@link PointCommandPayload} payload. Time fields use {@link Instant}
 * (UTC) uniformly.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
public record PointCommandDTO(
        String commandId,
        Long tenantId,
        PointCommandTypeEnum type,
        PointCommandPayload payload,
        PointCommandSourceEnum source,
        Long sourceUserId,
        Instant occurredAt,
        Instant expireAt,
        int schemaVersion
) {

    /**
     * Create a read command DTO with default source and timing.
     */
    public static PointCommandDTO ofRead(String commandId, Long tenantId, Long deviceId, Long pointId) {
        return new PointCommandDTO(
                commandId,
                tenantId,
                PointCommandTypeEnum.READ,
                new PointCommandPayload.ReadPayload(deviceId, pointId),
                PointCommandSourceEnum.HTTP,
                null,
                Instant.now(),
                Instant.now().plusSeconds(10),
                1
        );
    }

    /**
     * Create a write command DTO with default source and timing.
     */
    public static PointCommandDTO ofWrite(String commandId, Long tenantId, Long deviceId, Long pointId, String value) {
        return new PointCommandDTO(
                commandId,
                tenantId,
                PointCommandTypeEnum.WRITE,
                new PointCommandPayload.WritePayload(deviceId, pointId, value),
                PointCommandSourceEnum.HTTP,
                null,
                Instant.now(),
                Instant.now().plusSeconds(10),
                1
        );
    }
}
