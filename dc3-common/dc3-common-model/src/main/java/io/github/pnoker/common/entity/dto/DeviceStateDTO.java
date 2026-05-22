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

import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Device state heartbeat payload sent over RabbitMQ.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Driver ID that reports this state.
     */
    private Long driverId;

    /**
     * Tenant ID the device belongs to.
     */
    private Long tenantId;

    /**
     * Device ID
     */
    private Long deviceId;

    /**
     * Device status code (see {@link io.github.pnoker.common.enums.EntityStatusEnum})
     */
    private String status;

    /**
     * Lease timeout value.
     */
    @Builder.Default
    private int timeout = 15;

    /**
     * Lease timeout unit.
     */
    @Builder.Default
    private TimeUnit timeoutUnit = TimeUnit.MINUTES;

    /**
     * Structured description of the state (e.g. connectivity details, diagnostics).
     * Stored in {@code dc3_entity_state.entity_state_ext.content}.
     */
    private String stateDescription;

    /**
     * Create Time
     */
    private LocalDateTime createTime;

    public DeviceStateDTO(Long deviceId, String status) {
        this.deviceId = deviceId;
        this.status = status;
        this.createTime = LocalDateTimeUtil.now();
    }

    public DeviceStateDTO(Long deviceId, EntityStatusEnum status) {
        this(deviceId, status == null ? null : status.getCode());
    }

    public DeviceStateDTO(Long deviceId, String status, int timeout, TimeUnit timeoutUnit) {
        this.deviceId = deviceId;
        this.status = status;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.createTime = LocalDateTimeUtil.now();
    }

    public DeviceStateDTO(Long deviceId, EntityStatusEnum status, int timeout, TimeUnit timeoutUnit) {
        this(deviceId, status == null ? null : status.getCode(), timeout, timeoutUnit);
    }

}
