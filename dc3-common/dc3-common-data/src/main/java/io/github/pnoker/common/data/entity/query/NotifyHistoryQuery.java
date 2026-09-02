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
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Notify History offset query parameters")
public class NotifyHistoryQuery implements Serializable {

    @Schema(description = "Zero-based row offset", example = "0")
    private Long offset;

    @Schema(description = "Maximum rows to return", example = "50")
    @Builder.Default
    private Integer limit = PageRequest.DEFAULT_LIMIT;

    @Schema(description = "Whitelisted notification history sort fields")
    @Builder.Default
    private List<SortSpec> sort = List.of();

    /**
     * Null-safe paging accessors: the runtime Jackson 3 mapper binds request bodies
     * through the all-args constructor, leaving absent fields null. Boxed fields keep
     * "unspecified" distinguishable from explicit (possibly invalid) values.
     */
    public long getOffset() {
        return offset == null ? 0L : offset;
    }

    public int getLimit() {
        return limit == null ? PageRequest.DEFAULT_LIMIT : limit;
    }

    public List<SortSpec> getSort() {
        return sort == null ? List.of() : sort;
    }

    @Schema(description = "Filter by alarm rule identifier")
    private Long ruleId;

    @Schema(description = "Filter by notification policy identifier")
    private Long notifyId;

    @Schema(description = "Filter by message template identifier")
    private Long messageId;

    @Schema(description = "Filter by delivery channel identifier")
    private Long channelId;

    @Schema(description = "Filter by associated alarm identifier")
    private Long alarmId;

    @Schema(description = "Filter by notification channel type")
    private NotifyChannelTypeEnum channelTypeFlag;

    @Schema(description = "Filter by notification target text")
    private String target;

    @Schema(description = "Filter by delivery status")
    private NotifyHistoryStatusEnum statusFlag;
}
