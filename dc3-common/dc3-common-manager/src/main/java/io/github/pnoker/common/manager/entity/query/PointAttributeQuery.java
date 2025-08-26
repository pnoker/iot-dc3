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

package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 位号属性Query
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PointAttributeQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Pages page;

    /**
     * 租户ID
     */
    private Long tenantId;

    // 查询字段

    /**
     * 属性名称
     */
    private String attributeName;

    /**
     * 属性编号
     */
    private String attributeCode;

    /**
     * 属性类型标识
     */
    private AttributeTypeFlagEnum attributeTypeFlag;

    /**
     * 驱动ID
     */
    private Long driverId;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 版本
     */
    private Integer version;
}