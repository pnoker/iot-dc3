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

package io.github.pnoker.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.CredentialTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.RequirePasswordChangeFlagEnum;
import io.github.pnoker.common.enums.PasswordAlgorithmEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * View object for local login credentials.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Local credential view object")
public class LocalCredentialVO extends BaseVO {

    @Schema(description = "Identifier of the principal (user) this credential belongs to.", example = "1024")
    private Long principalId;

    @Schema(description = "Login name used to authenticate; unique within the tenant.", example = "alice")
    private String loginName;

    @Schema(description = "Type of credential, determines how authentication is performed.", example = "PASSWORD")
    private CredentialTypeEnum credentialType;

    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "Hashed credential secret; write-only, never returned in responses.", example = "$2a$10$examplehashedpassword")
    private String password;

    @Schema(description = "Hashing algorithm applied to the stored password.", example = "BCRYPT")
    private PasswordAlgorithmEnum passwordAlgorithm;

    @Schema(description = "Additional algorithm parameters for the password hash (e.g. cost factor, salt); stored as JSON.")
    private JsonExt passwordParams;

    @Schema(description = "Timestamp when the password was last changed.", example = "2025-01-15T10:30:00")
    private LocalDateTime passwordUpdatedTime;

    @Schema(description = "Timestamp after which the password is considered expired and must be changed.", example = "2026-01-15T10:30:00")
    private LocalDateTime passwordExpireTime;

    @Schema(description = "Number of consecutive failed authentication attempts since the last successful login.", example = "0")
    private Integer failedAttempts;

    @Schema(description = "Timestamp until which the credential is locked due to excessive failed attempts; null means not locked.", example = "2025-06-18T12:00:00")
    private LocalDateTime lockedUntil;

    @Schema(description = "Whether the user must change their password on next login.", example = "NOT_REQUIRED")
    private RequirePasswordChangeFlagEnum requirePasswordChange;

    @Schema(description = "Whether this credential is active; disabled credentials cannot authenticate.", example = "ENABLE")
    private EnableFlagEnum enableFlag;

    @Schema(description = "Additional credential metadata or extension attributes stored as JSON.")
    private JsonExt credentialExt;

}
