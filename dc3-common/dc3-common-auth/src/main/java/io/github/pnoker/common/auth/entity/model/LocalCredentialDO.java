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
 * Persistence object for the dc3_local_credential table.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@ToString
@TableName(value = "dc3_local_credential", autoResultMap = true)
public class LocalCredentialDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("principal_id")
    private Long principalId;

    @TableField("login_name")
    private String loginName;

    @TableField("login_name_normalized")
    private String loginNameNormalized;

    @TableField("credential_type")
    private String credentialType;

    @TableField("password_hash")
    @ToString.Exclude
    private String passwordHash;

    @TableField("password_algorithm")
    private String passwordAlgorithm;

    @TableField(value = "password_params", typeHandler = JacksonTypeHandler.class)
    private JsonExt passwordParams;

    @TableField("password_updated_time")
    private LocalDateTime passwordUpdatedTime;

    @TableField("password_expire_time")
    private LocalDateTime passwordExpireTime;

    @TableField("failed_attempts")
    private Integer failedAttempts;

    @TableField("locked_until")
    private LocalDateTime lockedUntil;

    @TableField("require_password_change")
    private Byte requirePasswordChange;

    @TableField("enable_flag")
    private Byte enableFlag;

    @TableField(value = "credential_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt credentialExt;

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
