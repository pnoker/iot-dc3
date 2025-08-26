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

package io.github.pnoker.common.dal.entity.bo;

import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.GroupTypeFlagEnum;
import lombok.*;

/**
 * Group BO
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class GroupBO extends BaseBO {

    /**
     * 父分组ID
     */
    private String parentGroupId;

    /**
     * 分组标识
     */
    private GroupTypeFlagEnum groupTypeFlag;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 分组编号
     */
    private String groupCode;

    /**
     * 分组层级
     */
    private Byte groupLevel;

    /**
     * 分组顺序
     */
    private Byte groupIndex;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;
}
