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
import io.github.pnoker.common.entity.ext.CommandExt;
import io.github.pnoker.common.enums.CallTypeEnum;
import io.github.pnoker.common.enums.CommandTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
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
 * View object for command API responses.
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
@Schema(description = "Command view object")
public class CommandVO extends BaseVO {

    @NotBlank(message = "Command name can't be empty", groups = {Add.class})
    @Schema(description = "Command name. Unique name within a tenant.", example = "Read Holding Register", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid command name format",
            groups = {Add.class, Update.class})
    private String commandName;

    @Schema(description = "Command code. Stable business identifier; must not change once deployed.", example = "READ_HOLDING_REG")

    private String commandCode;

    @Schema(description = "Command type: READ (fetch data) or WRITE (send data).", example = "READ")

    @NotNull(message = "Command type can't be empty", groups = {Add.class, Update.class})
    private CommandTypeEnum commandTypeFlag;

    @Schema(description = "Command invocation mode: SYNC (wait for response) or ASYNC (fire and forget).", example = "SYNC")

    @NotNull(message = "Call type can't be empty", groups = {Add.class, Update.class})
    private CallTypeEnum callTypeFlag;

    @Schema(description = "Command execution timeout in milliseconds. The command is cancelled if no response is received within this period.", example = "3000")

    @NotNull(message = "Command timeout can't be empty", groups = {Add.class, Update.class})
    private Integer timeout;

    @Schema(description = "Command extension information, serialized as JSON for custom parameters and metadata.")

    private CommandExt commandExt;

    @Schema(description = "ID of the profile (device template) this command is defined in.", example = "2048", requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "Profile ID can't be empty", groups = {Add.class, Update.class})
    private Long profileId;

    @Schema(description = "Enable flag: ENABLE (0) or DISABLE (1).", example = "ENABLE")

    private EnableFlagEnum enableFlag;

    @Schema(description = "Signature used for configuration integrity verification.")

    private String signature;

    @Schema(description = "Optimistic-lock version number for concurrent update control.", example = "1")

    private Integer version;

}
