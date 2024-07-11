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

package io.github.pnoker.common.entity.bo;

import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import lombok.*;

/**
 * Label BO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class LabelBO extends BaseBO {

    /**
     * 标签名称
     */
    private String labelName;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 实体标识
     */
    private EntityTypeFlagEnum entityTypeFlag;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;
}
