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

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Dictionary BO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryBO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典类型
     */
    private String type;

    /**
     * 字典标签名称
     */
    private String label;

    /**
     * 字典标签值
     */
    private String value;

    /**
     * 是否禁用
     */
    private boolean disabled;

    /**
     * 是否展开
     */
    private boolean expand;

    /**
     * 子节点
     */
    private List<DictionaryBO> children;
}
