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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.NotifyHistoryRequestExt;
import io.github.pnoker.common.entity.ext.NotifyHistoryResponseExt;
import io.github.pnoker.common.enums.NotifyChannelTypeFlagEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View object for notification delivery history API responses.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Notify History view object")
public class NotifyHistoryVO extends BaseVO {

    @Schema(description = "rule ID")

    private Long ruleId;

    @Schema(description = "notify ID")

    private Long notifyId;

    @Schema(description = "message ID")

    private Long messageId;

    @Schema(description = "channel ID")

    private Long channelId;

    @Schema(description = "alarm ID")

    private Long alarmId;

    @Schema(description = "channel type flag")

    private NotifyChannelTypeFlagEnum channelTypeFlag;

    @Schema(description = "target")

    private String target;

    @Schema(description = "status flag")

    private NotifyHistoryStatusEnum statusFlag;

    @Schema(description = "request extension information (JSON)")

    private NotifyHistoryRequestExt requestExt;

    @Schema(description = "response extension information (JSON)")

    private NotifyHistoryResponseExt responseExt;

    @Schema(description = "error message")

    private String errorMessage;

    @Schema(description = "retry count")

    private Integer retryCount;

}
