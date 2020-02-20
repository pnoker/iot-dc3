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

package com.pnoker.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pnoker.common.base.Converter;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.model.PointAttribute;
import lombok.*;
import org.springframework.beans.BeanUtils;

/**
 * ProfileInfo DTO
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PointAttributeDto extends PointAttribute implements Converter<PointAttribute, PointAttributeDto> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Pages page;

    @Override
    public void convertToDo(PointAttribute info) {
        BeanUtils.copyProperties(this, info);
    }

    @Override
    public PointAttributeDto convert(PointAttribute info) {
        BeanUtils.copyProperties(info, this);
        return this;
    }
}