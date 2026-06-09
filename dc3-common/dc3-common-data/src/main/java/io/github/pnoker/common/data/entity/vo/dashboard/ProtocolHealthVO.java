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

package io.github.pnoker.common.data.entity.vo.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Rollup for one driver service_name — how many driver rows are enabled, how many devices
 * they serve, how much sample volume they carried in the window. Phase-1 health signal is
 * just enable% + device count + sample volume; true heartbeat/latency signals get layered
 * in later.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Health rollup for one driver service name")
public class ProtocolHealthVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * e.g. {@code dc3-driver-modbus-tcp}; frontend strips the prefix.
     */
    @Schema(description = "driver service name, e.g. dc3-driver-modbus-tcp")
    private String serviceName;

    @Schema(description = "total driver count for this service")
    private long driverCount;

    @Schema(description = "enabled driver count for this service")
    private long enabledCount;

    @Schema(description = "device count served by this service")
    private long deviceCount;

}
