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

package io.github.pnoker.common.auth.entity.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.github.pnoker.common.entity.ext.JsonExt;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * Persistence object for the dc3_api table.
 *
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@TableName(value = "dc3_api", autoResultMap = true)
public class ApiDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Owning service name, populated by resource registrar
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * Type
     */
    @TableField("api_type_flag")
    private Byte apiTypeFlag;

    /**
     * Name
     */
    @TableField("api_name")
    private String apiName;

    /**
     * Code
     */
    @TableField("api_code")
    private String apiCode;

    /**
     * API grouping, usually the owning controller simple class name
     */
    @TableField("api_group")
    private String apiGroup;

    /**
     *
     */
    @TableField(value = "api_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt apiExt;

    /**
     * Enable flag, 0:Enable, 1:Disable
     */
    @TableField("enable_flag")
    private Byte enableFlag;

    /**
     * Description
     */
    @TableField("remark")
    private String remark;

    /**
     * Creator ID
     */
    @TableField("creator_id")
    private Long creatorId;

    /**
     * Creator Name
     */
    @TableField("creator_name")
    private String creatorName;

    /**
     * Create Time
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * Operator ID
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * Operator Name
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * Operate Time
     */
    @TableField("operate_time")
    private LocalDateTime operateTime;

    /**
     * Logical delete flag, 0:not deleted, 1:deleted
     */
    @TableLogic
    @TableField("deleted")
    private Byte deleted;

}
