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
 * Group view object (VO) used for API responses and client interactions.
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
@Schema(description = "Group view object")
public class GroupVO extends BaseVO {

    /**
     * Parent group ID.
     */
    @Schema(description = "ID of the parent group in the hierarchy. Null for root-level groups.", example = "1024")
    private Long parentGroupId;

    /**
     * Group type flag.
     */
    @Schema(description = "Group type enum")
    @NotNull(message = "Group type flag can't be empty", groups = {Add.class, Update.class})
    private EntityTypeEnum groupTypeFlag;

    /**
     * Group name.
     */
    @NotBlank(message = "Group name can't be empty", groups = {Add.class})
    @Schema(description = "Group name. Unique name within a tenant.", example = "Line 1 Devices", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid group name format",
            groups = {Add.class, Update.class})
    private String groupName;

    /**
     * Group code.
     */
    @Schema(description = "Group code. Stable business identifier.", example = "LINE_1")
    private String groupCode;

    /**
     * Group level.
     */
    @Schema(description = "Hierarchical depth level of this group. Root groups are level 0.", example = "1")
    private Byte groupLevel;

    /**
     * Group index/order.
     */
    @Schema(description = "Display ordering index among sibling groups.", example = "0")
    private Integer groupIndex;

    /**
     * Enable status flag.
     */
    @Schema(description = "Enable flag: ENABLE (0) or DISABLE (1).", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
