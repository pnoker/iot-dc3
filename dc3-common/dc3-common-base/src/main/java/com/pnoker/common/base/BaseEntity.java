/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.common.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>基础 domain 实体类
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Data
public class BaseEntity implements Serializable {
    private static final long serialVersionUID = -3969004704181842099L;

    /**
     * 主键，自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，关联操作到用户
     */
    private Long userId;

    /**
     * 描述信息
     */
    private String description;

    private Date createTime;
    private Date updateTime;

    /**
     * 逻辑删除标识 1：删除，0：未删除
     */
    @TableLogic
    private Integer deleted;
}
