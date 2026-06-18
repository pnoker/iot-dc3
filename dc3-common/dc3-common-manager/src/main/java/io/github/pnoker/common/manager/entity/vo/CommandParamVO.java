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
import io.github.pnoker.common.entity.ext.CommandParamExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ParamDirectionTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for command param API responses.
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
@Schema(description = "Command Param view object", example = "42")
public class CommandParamVO extends BaseVO {

    @NotBlank(message = "Param name can't be empty", groups = {Add.class})
    @Schema(description = "Command parameter name. Unique name within a command.", example = "Register Address", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid param name format",
            groups = {Add.class, Update.class})
    private String paramName;

    @Schema(description = "Command parameter code. Stable business identifier; must not change once deployed.", example = "REG_ADDR", requiredMode = Schema.RequiredMode.REQUIRED)

    @NotBlank(message = "Param code can't be empty", groups = {Add.class, Update.class})
    private String paramCode;

    @Schema(description = "Parameter direction: INPUT (sent to device) or OUTPUT (received from device).", example = "INPUT", requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "Param direction can't be empty", groups = {Add.class, Update.class})
    private ParamDirectionTypeEnum paramDirectionFlag;

    @Schema(description = "Parameter data type: STRING, INT, LONG, FLOAT, DOUBLE, or BOOL.", example = "INT", requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "Param type can't be empty", groups = {Add.class, Update.class})
    private PointTypeEnum paramTypeFlag;

    @Schema(description = "Whether this parameter is REQUIRED or OPTIONAL for command execution.", example = "REQUIRED")

    private Boolean requiredFlag;

    @Schema(description = "Default value pre-populated when the command is invoked without an explicit value.", example = "0")

    private String defaultValue;

    @Schema(description = "Command parameter extension information, serialized as JSON for custom metadata.")

    private CommandParamExt paramExt;

    @Schema(description = "ID of the parent command this parameter belongs to.", example = "4096", requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "Command ID can't be empty", groups = {Add.class, Update.class})
    private Long commandId;

    @Schema(description = "Enable flag: ENABLE (0) or DISABLE (1).", example = "ENABLE")

    private EnableFlagEnum enableFlag;

    @Schema(description = "Signature used for configuration integrity verification.")

    private String signature;

    @Schema(description = "Optimistic-lock version number for concurrent update control.", example = "1")

    private Integer version;

}
