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
 * Persistence object for the dc3_notify_history table.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
public class NotifyHistoryDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long ruleId;

    private Long notifyId;

    private Long messageId;

    private Long channelId;

    private Long alarmId;

    private String dedupeKey;

    private Byte channelTypeFlag;

    private String target;

    private Byte statusFlag;

    private JsonExt requestExt;

    private JsonExt responseExt;

    private String errorMessage;

    private Integer retryCount;

    private Long tenantId;

    private String remark;

    private Long creatorId;

    private String creatorName;

    private LocalDateTime createTime;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime operateTime;

}
