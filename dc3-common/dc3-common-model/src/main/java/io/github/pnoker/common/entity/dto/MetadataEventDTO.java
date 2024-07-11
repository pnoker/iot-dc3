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

package io.github.pnoker.common.entity.dto;

import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 元数据事件 DTO
 *
 * @author zhangzi
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetadataEventDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 根据元数据类型{@link MetadataTypeEnum}决定是驱动, 设备, 位号
     */
    private Long id;

    /**
     * 元数据类型
     */
    private MetadataTypeEnum metadataType;

    /**
     * 元数据操作类型, 新增, 删除, 修改
     */
    private MetadataOperateTypeEnum operateType;
}
