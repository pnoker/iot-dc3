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

package io.github.pnoker.common.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * LabelBind VO
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
public class LabelBindVO extends BaseVO {

    /**
     * 实体类型标识
     */
    @NotNull(message = "实体类型标识不能为空",
            groups = {Add.class, Update.class})
    private EntityTypeFlagEnum entityTypeFlag;

    /**
     * 标签ID
     */
    @NotNull(message = "标签ID不能为空",
            groups = {Add.class, Update.class})
    private Long labelId;

    /**
     * 实体ID
     */
    @NotNull(message = "实体ID不能为空",
            groups = {Add.class, Update.class})
    private Long entityId;
}
