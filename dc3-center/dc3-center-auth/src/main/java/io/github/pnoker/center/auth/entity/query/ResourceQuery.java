/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.entity.query;

import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.constant.enums.ResourceTypeFlagEnum;
import io.github.pnoker.common.entity.common.Pages;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * @author linys
 * @since 2022.1.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "ResourceQuery", description = "资源-查询")
public class ResourceQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private Pages page;

    /**
     * 权限资源父级ID
     */
    private String parentResourceId;

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
     * 权限资源范围标识，参考：ResourceScopeFlagEnum
     * <ul>
     *     <li>0x01：新增</li>
     *     <li>0x02：删除</li>
     *     <li>0x04：更新</li>
     *     <li>0x08：查询</li>
     * </ul>
     * 具有多个权限范围可以累加
     */
    private Byte resourceScopeFlag;

    /**
     * 权限资源实体ID
     */
    private String entityId;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;
}
