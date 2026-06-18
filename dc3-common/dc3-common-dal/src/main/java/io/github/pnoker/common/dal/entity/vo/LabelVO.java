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

package io.github.pnoker.common.dal.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
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
 * Label view object (VO) used for API responses and client interactions.
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
@Schema(description = "Label view object")
public class LabelVO extends BaseVO {

    /**
     * Label name.
     */
    @NotBlank(message = "Label name can't be empty", groups = {Add.class})
    @Schema(description = "Label name. Unique name within a tenant.", example = "Production Line A", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid label name format",
            groups = {Add.class, Update.class})
    private String labelName;

    /**
     * Label code.
     */
    @Schema(description = "Stable business code of the label; optional human-readable identifier distinct from labelName.", example = "LINE_A")
    private String labelCode;

    /**
     * Label color.
     */
    @Schema(description = "Display color for this label in UI (hex or named color).", example = "#FF6B6B")
    private String labelColor;

    /**
     * Entity type flag.
     */
    @Schema(description = "Type of the entity this label is attached to (e.g. DEVICE, POINT, DRIVER, PROFILE, COMMAND, EVENT).", example = "DEVICE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Entity type flag can't be empty", groups = {Add.class, Update.class})
    private EntityTypeEnum entityTypeFlag;

    /**
     * Enable status flag.
     */
    @Schema(description = "Whether the label is active; ENABLE to use it, DISABLE to hide it from selection.", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
