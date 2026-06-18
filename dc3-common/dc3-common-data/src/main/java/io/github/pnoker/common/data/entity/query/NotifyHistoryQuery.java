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

package io.github.pnoker.common.data.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Query parameters for notification delivery history listing and filtering.
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
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Notify History query parameters")
public class NotifyHistoryQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination parameters including page number, page size, sort order, and time range.")

    private Pages page;

    @Schema(description = "Tenant ID for multi-tenant isolation. Required for query scope.")

    private Long tenantId;

    @Schema(description = "rule ID", example = "1024")

    private Long ruleId;

    @Schema(description = "notify ID", example = "2048")

    private Long notifyId;

    @Schema(description = "message ID", example = "4096")

    private Long messageId;

    @Schema(description = "channel ID", example = "512")

    private Long channelId;

    @Schema(description = "alarm ID", example = "256")

    private Long alarmId;

    @Schema(description = "Notification channel type enum", example = "EMAIL")

    private NotifyChannelTypeEnum channelTypeFlag;

    @Schema(description = "Notification target", example = "admin@example.com")

    private String target;

    @Schema(description = "Status enum", example = "SUCCESS")

    private NotifyHistoryStatusEnum statusFlag;

}
