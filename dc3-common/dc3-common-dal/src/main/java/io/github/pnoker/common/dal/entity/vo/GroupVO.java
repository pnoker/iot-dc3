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
    @Schema(description = "parent group ID")
    private Long parentGroupId;

    /**
     * Group type flag.
     */
    @Schema(description = "group type flag")
    @NotNull(message = "Group type flag can't be empty", groups = {Add.class, Update.class})
    private EntityTypeEnum groupTypeFlag;

    /**
     * Group name.
     */
    @NotBlank(message = "Group name can't be empty", groups = {Add.class})
    @Schema(description = "group name")
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid group name format",
            groups = {Add.class, Update.class})
    private String groupName;

    /**
     * Group code.
     */
    @Schema(description = "group code")
    private String groupCode;

    /**
     * Group level.
     */
    @Schema(description = "group level")
    private Byte groupLevel;

    /**
     * Group index/order.
     */
    @Schema(description = "group index")
    private Integer groupIndex;

    /**
     * Enable status flag.
     */
    @Schema(description = "Enable flag: 0=enabled, 1=disabled")
    private EnableFlagEnum enableFlag;

}
