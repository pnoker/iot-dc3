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

package io.github.pnoker.center.manager.entity.ext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * Point Ext
 * <p>
 * 位号相关拓展信息
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "位号相关拓展")
public class PointExt implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 类型，用于解析Json字符串
     */
    @Schema(description = "类型")
    private String type;

    /**
     * 版本，用于乐观锁
     */
    @Schema(description = "版本")
    private Long version;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String remark;

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "内容")
    public static class Content {
        private String keep;
    }
}
