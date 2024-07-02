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

package io.github.pnoker.common.entity.ext;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Base Ext
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
public class BaseExt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 类型, 用于解析Json字符串
     */
    private String type;

    /**
     * 版本, 用于乐观锁
     */
    @Builder.Default
    private Integer version = 1;

    /**
     * 描述
     */
    private String remark;
}
