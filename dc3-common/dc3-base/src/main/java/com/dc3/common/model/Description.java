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

package com.dc3.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.dc3.common.constant.Common;
import com.dc3.common.valid.Update;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 基础 domain 实体类
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Description implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键，自增ID
     */
    @TableId(type = IdType.AUTO)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @NotNull(message = "id can't be empty", groups = {Update.class})
    private Long id;

    /**
     * 描述信息
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = Common.DATE_FORMAT, timezone = Common.TIMEZONE)
    private Date createTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = Common.DATE_FORMAT, timezone = Common.TIMEZONE)
    private Date updateTime;

    /**
     * 逻辑删除标识
     * 1：删除
     * 0：未删除
     */
    @TableLogic
    @TableField(select = false)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private Integer deleted;
}
