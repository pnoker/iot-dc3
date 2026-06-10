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

package io.github.pnoker.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.ResourceExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceScopeTypeEnum;
import io.github.pnoker.common.enums.ResourceTypeEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Auth;
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
 * View object for resource API responses.
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
@Schema(description = "Resource view object")
public class ResourceVO extends BaseVO {

    /**
     * ID
     */
    @Schema(description = "Parent resource ID", example = "1024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Resource parent id can't be empty", groups = {Add.class, Update.class})
    private Long parentResourceId;

    /**
     * Name
     */
    @NotBlank(message = "Role name can't be empty", groups = {Add.class, Auth.class})
    @Schema(description = "Resource name", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid role name",
            groups = {Add.class, Update.class})
    private String resourceName;

    /**
     * Code
     */
    @Schema(description = "Resource permission code")
    private String resourceCode;

    /**
     * Service name.
     */
    @Schema(description = "Service name")
    private String serviceName;

    /**
     * Type
     */
    @Schema(description = "Resource type enum", example = "MENU")
    private ResourceTypeEnum resourceTypeFlag;

    /**
     * , : ResourceScopeTypeEnum
     * <ul>
     * <li>0x01:</li>
     * <li>0x02:</li>
     * <li>0x04:</li>
     * <li>0x08:</li>
     * </ul>
     *
     */
    @Schema(description = "Resource scope enum", example = "LIST")
    private ResourceScopeTypeEnum resourceScopeFlag;

    /**
     * Entity ID
     */
    @Schema(description = "Associated entity ID", example = "1024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Entity ID can't be empty", groups = {Add.class, Update.class})
    private Long entityId;

    /**
     *
     */
    @Schema(description = "Resource extension information in JSON format")
    private ResourceExt resourceExt;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag enum (ENABLE or DISABLE)", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
