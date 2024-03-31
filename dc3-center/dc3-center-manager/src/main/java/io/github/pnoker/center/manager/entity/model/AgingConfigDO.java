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

package io.github.pnoker.center.manager.entity.model;

import com.baomidou.mybatisplus.annotation.*;
import io.github.pnoker.common.entity.common.Pages;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 时效配置表
 * </p>
 *
 * @author pnoker
 * @since 2024-03-31
 */
@Getter
@Setter
@TableName("aging_config")
public class AgingConfigDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 消费者ID
     */
    @TableField("consume_id")
    private String consumeId;

    /**
     * 时效参数JSON
     */
    @TableField("aging_json")
    private String agingJson;

    /**
     * 描述信息
     */
    @TableField("description")
    private String description;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 删除0：未删除；1：删除；
     */
    @TableField("deleted")
    @TableLogic
    private Byte deleted;

    @Transient
    private Pages page;
}
