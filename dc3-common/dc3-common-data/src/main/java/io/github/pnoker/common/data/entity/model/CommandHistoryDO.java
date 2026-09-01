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

import io.github.pnoker.common.enums.CommandHistorySourceEnum;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Persistence object for the dc3_command_history table.
 *
 * @author pnoker
 * @since 2026.5.23
 */
@Getter
@Setter
@ToString
public class CommandHistoryDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String recordId;

    private Long tenantId;

    private Long deviceId;

    private Long commandId;

    private String commandCode;

    private String paramValues;

    private String resultValues;

    private String configSnapshot;

    private PointCommandStatusEnum status;

    private String errorCode;

    private String errorMessage;

    private CommandHistorySourceEnum source;

    private Long sourceUserId;

    private LocalDateTime occurTime;

    private LocalDateTime sendTime;

    private LocalDateTime finishTime;

    private LocalDateTime expireTime;

    private Short schemaVersion;

    private LocalDateTime createTime;

    private LocalDateTime operateTime;

}
