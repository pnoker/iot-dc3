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
import io.github.pnoker.center.manager.entity.bo.LabelBO;
import io.github.pnoker.center.manager.entity.vo.LabelVO;
import io.github.pnoker.common.entity.common.Pages;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Label DTO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@SuperBuilder
@RequiredArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "LabelQuery", description = "标签-查询")
public class LabelQuery extends LabelVO {

    @Schema(description = "分页")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Pages page;
}