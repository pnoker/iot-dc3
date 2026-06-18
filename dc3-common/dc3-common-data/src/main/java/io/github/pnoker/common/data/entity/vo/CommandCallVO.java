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
 * VO for submitting a custom command call.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Getter
@Setter
@ToString
@Schema(description = "Command Call view object")
public class CommandCallVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Identifier of the target device; must belong to the current tenant.", example = "1024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long deviceId;

    @Schema(description = "Identifier of the command being invoked; must reference a command accessible to the current tenant.", example = "4096")

    private Long commandId;

    @Schema(description = "Command code. Stable business identifier matching the command definition.", example = "READ_HOLDING_REG")

    private String commandCode;

    @Schema(description = "Parameter values for this command invocation, keyed by parameter code.")
    private Map<String, String> paramValues;

    @Schema(description = "Client-side correlation ID used to match this call with its asynchronous response.", example = "cmd-req-9f3a7c2d")
    private String commandId_;

}
