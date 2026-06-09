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

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(description = "device ID", example = "1024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long deviceId;

    @Schema(description = "command ID")

    private Long commandId;

    @Schema(description = "command code")

    private String commandCode;

    @Schema(description = "command parameter values")
    private Map<String, String> paramValues;

    @Schema(description = "command id_")

    private String commandId_;

}
