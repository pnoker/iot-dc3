/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.common.agentic.entity.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agentic model option metadata.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
@ToString
@TableName("dc3_model_config")
public class ModelConfigDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID.
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Model identifier, as accepted by the provider's API.
     */
    @TableField("model")
    private String model;

    /**
     * Human-readable model label shown in the model picker.
     */
    @TableField("label")
    private String label;

    /**
     * Provider that serves this model.
     */
    @TableField("provider_id")
    private Long providerId;

    /**
     * Whether the model supports streaming responses.
     */
    @TableField("stream")
    private Boolean stream;

    /**
     * Whether the model supports tool/function calling.
     */
    @TableField("tool_call")
    private Boolean toolCall;

    /**
     * Whether the model supports image/vision inputs.
     */
    @TableField("vision")
    private Boolean vision;

    /**
     * Whether the model exposes a separate reasoning trace.
     */
    @TableField("reasoning")
    private Boolean reasoning;

    /**
     * Sampling temperature; null lets the provider default apply.
     */
    @TableField("temperature")
    private Double temperature;

    /**
     * Maximum output tokens; null lets the provider default apply.
     */
    @TableField("max_tokens")
    private Integer maxTokens;

    /**
     * Default flag, 0:Not default, 1:Default.
     */
    @TableField("default_flag")
    private Byte defaultFlag;

    /**
     * Enable flag, 0:Enable, 1:Disable.
     */
    @TableField("enable_flag")
    private Byte enableFlag;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("remark")
    private String remark;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("creator_name")
    private String creatorName;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("operate_time")
    private LocalDateTime operateTime;

    @TableLogic
    @TableField("deleted")
    private Byte deleted;

}
