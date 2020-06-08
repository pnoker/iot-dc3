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

package com.dc3.common.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.dc3.common.valid.Insert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * 字典表
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Dictionary {
    @NotBlank(message = "label can't be empty", groups = {Insert.class})
    private String label;

    @NotNull(message = "value can't be empty", groups = {Insert.class})
    private Long value;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean disabled;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean expand = true;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String type;

    @TableField(exist = false)
    private List<Dictionary> children = new ArrayList<>();
}
