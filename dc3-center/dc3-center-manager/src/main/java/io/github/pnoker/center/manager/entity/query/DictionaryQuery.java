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

package io.github.pnoker.center.manager.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.valid.Parent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 字典查询实体类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@SuperBuilder
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "GroupQuery", description = "字典-查询")
public class DictionaryQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "分页")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Pages page;

    @Schema(description = "名称")
    private String label;

    @NotNull(message = "父级ID不能为空",
            groups = {Parent.class})
    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "租户ID")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private Long tenantId;
}