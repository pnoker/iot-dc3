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

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Persistence object for the dc3_event_history table.
 *
 * @author pnoker
 * @since 2026.5.23
 */
@Getter
@Setter
@ToString
public class EventHistoryDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String recordId;

    private Long tenantId;

    private Long deviceId;

    private Long eventId;

    private String eventCode;

    private Byte eventTypeFlag;

    private Byte eventLevelFlag;

    private String paramValues;

    private String configSnapshot;

    private String message;

    private LocalDateTime occurTime;

    private LocalDateTime receiveTime;

    private Byte acknowledgeFlag;

    private LocalDateTime acknowledgeTime;

    private Long acknowledgeUserId;

    private Short schemaVersion;

    private LocalDateTime createTime;

    private LocalDateTime operateTime;
}
