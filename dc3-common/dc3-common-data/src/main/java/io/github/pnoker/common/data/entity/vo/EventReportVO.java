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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * VO for reporting an event from a device or external system.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Getter
@Setter
@ToString
@Schema(description = "Event Report view object")
public class EventReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Identifier of the reporting device; must belong to the current tenant.", example = "1024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long deviceId;

    @Schema(description = "Identifier of the event definition being reported; must belong to the current tenant.", example = "4096")
    private Long eventId;

    @Schema(description = "Stable business code of the event; must match a defined event code.", example = "HIGH_TEMP_ALARM")
    private String eventCode;

    @Schema(description = "Parameter values submitted with this event report, keyed by parameter code.")
    private Map<String, String> paramValues;

    @Schema(description = "Free-text detail or payload accompanying the event report.", example = "Temperature exceeded the configured threshold")
    private String message;

}
