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
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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

    @Schema(description = "ID of the rule that triggered this notification.", example = "1024")
    private Long ruleId;

    @Schema(description = "ID of the notification definition used.", example = "2048")
    private Long notifyId;

    @Schema(description = "ID of the message template used for this notification.", example = "4096")
    private Long messageId;

    @Schema(description = "ID of the notification channel through which this notification was delivered.", example = "512")
    private Long channelId;

    @Schema(description = "ID of the alarm associated with this notification.", example = "512")
    private Long alarmId;

    @Schema(description = "Channel type used to deliver this notification (e.g. EMAIL, SMS, WEBHOOK).", example = "EMAIL")
    private NotifyChannelTypeEnum channelTypeFlag;

    @Schema(description = "Notification target address or identifier (e.g. email address, phone number, webhook URL).", example = "admin@example.com")
    private String target;

    @Schema(description = "Delivery status of this notification record (e.g. SUCCESS, FAILED, PENDING).", example = "SUCCESS")
    private NotifyHistoryStatusEnum statusFlag;

    @Schema(description = "Request payload sent to the notification channel, serialized as JSON.")
    private NotifyHistoryRequestExt requestExt;

    @Schema(description = "Response received from the notification channel, serialized as JSON.")
    private NotifyHistoryResponseExt responseExt;

    @Schema(description = "Error message if the notification delivery failed. Null on success.", example = "SMTP connection refused")
    private String errorMessage;

    @Schema(description = "Number of delivery retries attempted for this notification.", example = "0")
    private Integer retryCount;

}
