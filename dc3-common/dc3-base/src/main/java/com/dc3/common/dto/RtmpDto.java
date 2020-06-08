/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.common.dto;

import com.dc3.common.base.Converter;
import com.dc3.common.bean.Pages;
import com.dc3.common.model.Rtmp;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * Rtmp DTO
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
public class RtmpDto implements Serializable, Converter<Rtmp, RtmpDto> {
    private static final long serialVersionUID = 1L;

    private String name;
    private Short videoType;
    private Boolean run;
    private Boolean autoStart;
    private Long userId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Pages page;

    public RtmpDto(boolean autoStart) {
        this.autoStart = autoStart;
    }

    @Override
    public void convertToDo(Rtmp rtmp) {
        BeanUtils.copyProperties(this, rtmp);
    }

    @Override
    public RtmpDto convert(Rtmp rtmp) {
        BeanUtils.copyProperties(rtmp, this);
        return this;
    }
}