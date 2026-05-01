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

package io.github.pnoker.common.auth.entity.bo;

import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.entity.ext.ResourceExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceScopeFlagEnum;
import io.github.pnoker.common.enums.ResourceTypeFlagEnum;
import lombok.*;

/**
 * Resource BO
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class ResourceBO extends BaseBO {

    /**
     * ID
     */
    private Long parentResourceId;

    /**
     * Name
     */
    private String resourceName;

    /**
     * Code
     */
    private String resourceCode;

    /**
     * Type
     */
    private ResourceTypeFlagEnum resourceTypeFlag;

    /**
     * , : ResourceScopeFlagEnum
     * <ul>
     *   <li>0x01: </li>
     *   <li>0x02: </li>
     *   <li>0x04: </li>
     *   <li>0x08: </li>
     * </ul>
     *
     */
    private ResourceScopeFlagEnum resourceScopeFlag;

    /**
     * Entity ID
     */
    private Long entityId;

    /**
     *
     */
    private ResourceExt resourceExt;

    /**
     * Enable flag
     */
    private EnableFlagEnum enableFlag;

}
