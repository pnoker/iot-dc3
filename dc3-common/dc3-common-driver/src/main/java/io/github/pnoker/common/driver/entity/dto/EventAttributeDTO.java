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

package io.github.pnoker.common.driver.entity.dto;

import io.github.pnoker.common.entity.base.BaseDTO;
import io.github.pnoker.common.entity.ext.EventAttributeExt;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object that describes a event-level attribute definition.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventAttributeDTO extends BaseDTO {

    /**
     * Attribute display name.
     */
    private String attributeName;

    /**
     * Attribute code used in configuration maps.
     */
    private String attributeCode;

    /**
     * Attribute value type.
     */
    private AttributeTypeEnum attributeTypeFlag;

    /**
     * Default value used when no explicit configuration is provided.
     */
    private String defaultValue;

    /**
     * Driver ID
     */
    private Long driverId;

    /**
     * Extended attribute metadata.
     */
    private EventAttributeExt attributeExt;

    /**
     * Enable flag
     */
    private EnableFlagEnum enableFlag;

    /**
     * Tenant identifier.
     */
    private Long tenantId;

    /**
     * Data signature used for optimistic checks or synchronization.
     */
    private String signature;

    /**
     * Data version.
     */
    private Integer version;

}
