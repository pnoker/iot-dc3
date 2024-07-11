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

package io.github.pnoker.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.ResourceExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceScopeFlagEnum;
import io.github.pnoker.common.enums.ResourceTypeFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Auth;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Resource VO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ResourceVO extends BaseVO {

    /**
     * 权限资源父级ID
     */
    @NotBlank(message = "Resource parent id can't be empty",
            groups = {Add.class, Update.class})
    private Long parentResourceId;

    /**
     * 权限资源名称
     */
    @NotBlank(message = "Role name can't be empty",
            groups = {Add.class, Auth.class})
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$",
            message = "Invalid role name",
            groups = {Add.class, Update.class})
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
    private ResourceScopeFlagEnum resourceScopeFlag;

    /**
     * 权限资源实体ID
     */
    @NotNull(message = "实体ID不能为空",
            groups = {Add.class, Update.class})
    private Long entityId;

    /**
     * 资源拓展信息
     */
    private ResourceExt resourceExt;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;
}
