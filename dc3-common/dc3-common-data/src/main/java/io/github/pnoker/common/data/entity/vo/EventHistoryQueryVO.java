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

import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * VO for querying event records with pagination and filters.
 *
 * @author pnoker
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
    private String deviceId;

    @Schema(
            description = "Identifier of the event definition to filter by; must belong to the current tenant.",
            example = "4096")
    private String eventId;

    @Schema(description = "Code of the event to filter by.", example = "HIGH_TEMP_ALARM")
    private String eventCode;

    @Schema(description = "Type of the event to filter by.", example = "ALERT")
    private EventTypeFlagEnum eventTypeFlag;

    @Schema(description = "Zero-based result offset.", example = "0")
    private long offset;

    @Schema(description = "Maximum number of results.", example = "50")
    private int limit = PageRequest.DEFAULT_LIMIT;

    @Schema(description = "Stable sort fields; defaults to occurTime DESC, id DESC.")
    private java.util.List<SortSpec> sort = java.util.List.of();
}
