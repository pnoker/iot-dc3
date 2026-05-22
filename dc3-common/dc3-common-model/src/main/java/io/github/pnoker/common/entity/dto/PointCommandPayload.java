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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed interface for point command payload types, replacing the previous
 * {@code content} JSON-string pattern. Jackson polymorphic deserialization
 * uses the {@code kind} property to select the concrete record type.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PointCommandPayload.ReadPayload.class, name = "ReadPayload"),
        @JsonSubTypes.Type(value = PointCommandPayload.WritePayload.class, name = "WritePayload")
})
public sealed interface PointCommandPayload
        permits PointCommandPayload.ReadPayload, PointCommandPayload.WritePayload {

    /**
     * Read command payload: which device and point to read from.
     */
    record ReadPayload(Long deviceId, Long pointId) implements PointCommandPayload {
    }

    /**
     * Write command payload: which device/point to write to and the value.
     */
    record WritePayload(Long deviceId, Long pointId, String value) implements PointCommandPayload {
    }
}
