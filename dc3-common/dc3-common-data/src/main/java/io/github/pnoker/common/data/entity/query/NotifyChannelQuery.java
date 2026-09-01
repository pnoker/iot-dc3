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
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Query parameters for notification channel listing and filtering.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Notify Channel query parameters")
public class NotifyChannelQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Zero-based number of records to skip.")
    private long offset;

    @Schema(description = "Maximum number of records to return.", maximum = "200")
    @Builder.Default
    private int limit = PageRequest.DEFAULT_LIMIT;

    @Schema(description = "Stable allow-listed sort fields.")
    @Builder.Default
    private List<SortSpec> sort = List.of();

    @Schema(description = "Tenant ID for multi-tenant isolation. Required for query scope.")
    private Long tenantId;

    @Schema(description = "Filter by notification channel name. Supports partial matching.", example = "SMS Alert Channel")
    private String channelName;

    @Schema(description = "Filter by notification channel code. Exact match on the stable business identifier.", example = "SMS_CHANNEL")
    private String channelCode;

    @Schema(description = "Filter by notification channel type (e.g. EMAIL, SMS, WEBHOOK, MESSAGE_BUS). Exact match.")
    private NotifyChannelTypeEnum channelTypeFlag;

    @Schema(description = "Enable flag: ENABLE (0) or DISABLE (1).", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
