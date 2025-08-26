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

package io.github.pnoker.common.auth.entity.query;

import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceTypeFlagEnum;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author linys
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ResourceQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Pages page;

    /**
     * 租户ID
     */
    private Long tenantId;

    // 查询字段

    /**
     * 权限资源名称
     */
    private String resourceName;

    /**
     * 权限资源编号
     */
    private String resourceCode;

    /**
     * 权限资源类型标识
     */
    private ResourceTypeFlagEnum resourceTypeFlag;

    /**
     * 权限资源范围标识, 参考: ResourceScopeFlagEnum
     * <ul>
     *     <li>0x01: 新增</li>
     *     <li>0x02: 删除</li>
     *     <li>0x04: 更新</li>
     *     <li>0x08: 查询</li>
     * </ul>
     * 具有多个权限范围可以累加
     */
    private Byte resourceScopeFlag;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;
}
