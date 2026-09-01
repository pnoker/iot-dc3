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

package io.github.pnoker.common.data.entity.model;

import io.github.pnoker.common.entity.ext.JsonExt;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Persistence object for the dc3_entity_alarm table.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
public class EntityAlarmDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID
     */
    private Long id;

    /**
     * Alarm target type flag, 0: point, 1: device, 2: driver
     */
    private Byte alarmTargetTypeFlag;

    /**
     * Alarm target entity ID
     */
    private Long entityId;

    /**
     * Driver ID
     */
    private Long driverId;

    /**
     * Device ID
     */
    private Long deviceId;

    /**
     * Point ID
     */
    private Long pointId;

    /**
     * Rule ID
     */
    private Long ruleId;

    /**
     * Rule state ID
     */
    private Long ruleStateId;

    private String dedupeKey;

    /**
     * Alarm type flag, 0: rule, 1: offline, 2: fault, 3: state flip, 4: report
     */
    private Byte alarmTypeFlag;

    /**
     * Alarm source flag, 0: rule, 1: state timeout, 2: device report, 3: driver report, 4: system
     */
    private Byte alarmSourceFlag;

    /**
     * Alarm level flag, 0: P0, 1: P1, 2: P2, 3: P3
     */
    private Byte alarmLevelFlag;

    /**
     * Alarm extension information
     */
    private JsonExt alarmExt;

    /**
     * Expiration duration, seconds
     */
    private Long expiredTime;

    /**
     * Confirmation flag, 0: unconfirmed, 1: confirmed
     */
    private Byte confirmFlag;

    /**
     * Tenant ID
     */
    private Long tenantId;

    /**
     * Create Time
     */
    private LocalDateTime createTime;

    /**
     * Operate Time
     */
    private LocalDateTime operateTime;

}
