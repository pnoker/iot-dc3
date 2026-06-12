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
 * Persistence object for identity providers.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@ToString
@TableName(value = "dc3_identity_provider", autoResultMap = true)
public class IdentityProviderDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("provider_code")
    private String providerCode;

    @TableField("provider_name")
    private String providerName;

    @TableField("provider_type")
    private String providerType;

    @TableField("issuer")
    private String issuer;

    @TableField("discovery_url")
    private String discoveryUrl;

    @TableField("authorization_uri")
    private String authorizationUri;

    @TableField("token_uri")
    private String tokenUri;

    @TableField("user_info_uri")
    private String userInfoUri;

    @TableField("jwks_uri")
    private String jwksUri;

    @TableField("client_id")
    private String clientId;

    @TableField("client_secret_ref")
    @ToString.Exclude
    private String clientSecretRef;

    @TableField("scopes")
    private String scopes;

    @TableField("redirect_uri")
    private String redirectUri;

    @TableField("subject_claim")
    private String subjectClaim;

    @TableField("username_claim")
    private String usernameClaim;

    @TableField("email_claim")
    private String emailClaim;

    @TableField(value = "attribute_mapping", typeHandler = JacksonTypeHandler.class)
    private JsonExt attributeMapping;

    @TableField("provisioning_mode")
    private String provisioningMode;

    @TableField("enable_flag")
    private Byte enableFlag;

    @TableField(value = "provider_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt providerExt;

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
