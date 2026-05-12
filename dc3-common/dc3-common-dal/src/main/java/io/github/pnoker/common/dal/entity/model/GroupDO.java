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

package io.github.pnoker.common.dal.entity.model;

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
 * <p>
 * Group data table entity.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@ToString
@TableName("dc3_group")
public class GroupDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID.
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Parent group ID.
     */
    @TableField("parent_group_id")
    private Long parentGroupId;

    /**
     * Group type flag.
     */
    @TableField("entity_type_flag")
    private Byte groupTypeFlag;

    /**
     * Group name.
     */
    @TableField("group_name")
    private String groupName;

    /**
     * Group code.
     */
    @TableField("group_code")
    private String groupCode;

    /**
     * Group level.
     */
    @TableField("group_level")
    private Byte groupLevel;

    /**
     * Group index/order.
     */
    @TableField("group_index")
    private Byte groupIndex;

    /**
     * Enable flag, {@code 0} for enabled, {@code 1} for disabled.
     */
    @TableField("enable_flag")
    private Byte enableFlag;

    /**
     * Tenant ID.
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * Remark or description.
     */
    @TableField("remark")
    private String remark;

    /**
     * Creator ID.
     */
    @TableField("creator_id")
    private Long creatorId;

    /**
     * Creator name.
     */
    @TableField("creator_name")
    private String creatorName;

    /**
     * Creation time.
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * Operator ID.
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * Operator name.
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * Operation time.
     */
    @TableField("operate_time")
    private LocalDateTime operateTime;

    /**
     * Logical delete flag, {@code 0} for not deleted, {@code 1} for deleted.
     */
    @TableLogic
    @TableField("deleted")
    private Byte deleted;

}
