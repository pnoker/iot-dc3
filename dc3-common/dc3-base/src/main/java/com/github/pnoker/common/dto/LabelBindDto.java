/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pnoker.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.pnoker.common.base.Converter;
import com.github.pnoker.common.bean.Pages;
import com.github.pnoker.common.model.LabelBind;
import lombok.*;
import org.springframework.beans.BeanUtils;

/**
 * LabelBind DTO
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LabelBindDto extends LabelBind implements Converter<LabelBind, LabelBindDto> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Pages page;

    @Override
    public void convertToDo(LabelBind bind) {
        BeanUtils.copyProperties(this, bind);
    }

    @Override
    public LabelBindDto convert(LabelBind bind) {
        BeanUtils.copyProperties(bind, this);
        return this;
    }
}