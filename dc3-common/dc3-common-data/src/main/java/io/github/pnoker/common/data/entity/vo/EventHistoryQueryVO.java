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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.utils.PageUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * VO for querying event records with pagination and filters.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Getter
@Setter
@ToString
@Schema(description = "Event History view object")
public class EventHistoryQueryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Identifier of the device to filter by; must belong to the current tenant.", example = "1024")
    private Long deviceId;

    @Schema(description = "Identifier of the event definition to filter by; must belong to the current tenant.", example = "4096")
    private Long eventId;

    @Schema(description = "Code of the event to filter by.", example = "HIGH_TEMP_ALARM")
    private String eventCode;

    @Schema(description = "Type of the event to filter by.", example = "ALERT")
    private EventTypeFlagEnum eventTypeFlag;

    @Schema(description = "Pagination parameters: page number and page size.")
    private Pages page;

    public <T> Page<T> toPage() {
        return PageUtil.page(page);
    }

}
