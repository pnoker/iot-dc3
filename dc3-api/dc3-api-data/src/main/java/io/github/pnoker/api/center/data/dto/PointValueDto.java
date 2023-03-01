/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.api.center.data.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.base.Converter;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.point.PointValue;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointValueDto implements Serializable, Converter<PointValue> {
    private static final long serialVersionUID = 1L;

    private String deviceId;
    private String pointId;

    /**
     * 位号名称
     */
    private String pointName;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 是否返回最近历史数据
     */
    private Boolean history = false;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Pages page;

    @Override
    public void convertDtoToDo(PointValue pointValue) {
        BeanUtils.copyProperties(this, pointValue);
    }

    @Override
    public void convertDoToDto(PointValue pointValue) {
        BeanUtils.copyProperties(pointValue, this);
    }
}
