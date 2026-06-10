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

package io.github.pnoker.common.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.EventParamExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View object for event param API responses.
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
@Schema(description = "Event Param view object")
public class EventParamVO extends BaseVO {

    @NotBlank(message = "Param name can't be empty", groups = {Add.class})
    @Schema(description = "param name")
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid param name format",
            groups = {Add.class, Update.class})
    private String paramName;

    @Schema(description = "param code")

    @NotBlank(message = "Param code can't be empty", groups = {Add.class, Update.class})
    private String paramCode;

    @Schema(description = "Parameter type enum")

    @NotNull(message = "Param type can't be empty", groups = {Add.class, Update.class})
    private PointTypeEnum paramTypeFlag;

    @Schema(description = "param extension information in JSON format")

    private EventParamExt paramExt;

    @Schema(description = "event ID")

    @NotNull(message = "Event ID can't be empty", groups = {Add.class, Update.class})
    private Long eventId;

    @Schema(description = "Enable flag enum (ENABLE or DISABLE)")

    private EnableFlagEnum enableFlag;

    @Schema(description = "Configuration signature")

    private String signature;

    @Schema(description = "Version number")

    private Integer version;

}
