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
package io.github.pnoker.common.data.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Detailed online-status lease of one device: current status plus the heartbeat and
 * lease-expiry times the status was derived from. Powers the device detail dashboard's
 * status banner.
 *
 * @author pnoker
 * @since 2026.9.24
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Detailed online-status lease of one device")
public class DeviceStatusDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "device ID", example = "1024")
    private String deviceId;

    /**
     * Current status code (ONLINE, OFFLINE, MAINTAIN, FAULT); OFFLINE when the device has
     * no lease row or its lease has expired.
     */
    @Schema(description = "current status code (ONLINE, OFFLINE, MAINTAIN, FAULT)", example = "ONLINE")
    private String status;

    @Schema(description = "latest heartbeat time; null when the device never reported")
    private LocalDateTime lastHeartbeatTime;

    @Schema(description = "heartbeat timeout in seconds used for this lease")
    private Integer timeoutSeconds;

    @Schema(description = "absolute time when this lease expires; null when the device never reported")
    private LocalDateTime expireTime;
}
