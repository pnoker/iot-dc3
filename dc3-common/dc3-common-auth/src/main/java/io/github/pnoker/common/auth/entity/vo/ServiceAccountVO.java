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
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * View object for service accounts.
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
@Schema(description = "Service account view object")
public class ServiceAccountVO extends BaseVO {

    @Schema(description = "Identifier of the service account principal", example = "1024")
    private Long principalId;

    @Schema(description = "Identifier of the owning tenant", example = "1")
    private Long tenantId;

    @Schema(description = "Human-readable name of the service account", example = "data-pipeline-bot")
    private String serviceAccountName;

    @Schema(description = "Identifier of the owning user principal", example = "2048")
    private Long ownerPrincipalId;

    @Schema(description = "Description of the service account's intended use", example = "Token used by the nightly ETL job")
    private String purpose;

    @Schema(description = "Optional expiry timestamp; null means the account never expires", example = "2026-12-31T23:59:59")
    private LocalDateTime expireTime;

    @Schema(description = "Timestamp of the most recent credential use; null if never used", example = "2026-06-18T08:30:00")
    private LocalDateTime lastUsedTime;

    @Schema(description = "Structured credential policy (rotation, scope, etc.) for this service account")
    private JsonExt credentialPolicyExt;

    @Schema(description = "Whether the service account is enabled", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
