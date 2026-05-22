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

/**
 * Driver alarm payload sent over RabbitMQ.
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
public class DriverAlarmDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Tenant ID the driver belongs to.
     */
    private Long tenantId;

    /**
     * Driver ID
     */
    private Long driverId;

    /**
     * Driver status code (see {@link io.github.pnoker.common.enums.EntityStatusEnum}), may be null.
     */
    private String status;

    /**
     * Driver status name, may be null.
     */
    private String statusName;

    /**
     * Human-readable alarm message.
     */
    private String message;

    /**
     * Alarm Id
     */
    private Long alarmId;

    /**
     * Create Time
     */
    private LocalDateTime createTime;

    public DriverAlarmDTO(Long driverId, String message) {
        this.driverId = driverId;
        this.message = message;
        this.createTime = LocalDateTimeUtil.now();
    }

}
