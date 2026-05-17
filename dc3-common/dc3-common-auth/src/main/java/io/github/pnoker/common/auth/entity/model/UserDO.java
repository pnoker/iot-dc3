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
 * User data table entity.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@TableName(value = "dc3_user", autoResultMap = true)
public class UserDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID.
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Username.
     */
    @TableField("user_name")
    private String userName;

    /**
     * User nickname.
     */
    @TableField("nick_name")
    private String nickName;

    /**
     * Phone number.
     */
    @TableField("phone")
    private String phone;

    /**
     * Email address.
     */
    @TableField("email")
    private String email;

    /**
     * Social-related extension information.
     */
    @TableField(value = "social_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt socialExt;

    /**
     * Identity-related extension information.
     */
    @TableField(value = "identity_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt identityExt;

    /**
     * Enable flag, {@code 0} for enabled, {@code 1} for disabled.
     */
    @TableField("enable_flag")
    private Byte enableFlag;

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
