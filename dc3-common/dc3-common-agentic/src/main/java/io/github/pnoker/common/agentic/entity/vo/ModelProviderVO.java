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
package io.github.pnoker.common.agentic.entity.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import io.github.pnoker.common.enums.ConnectivityStatusEnum;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for agentic model provider API responses.
 *
 * @author pnoker
 * @since 2026.5.10
 */
@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "Model Provider view object")
public class ModelProviderVO extends BaseVO {

    @Schema(description = "Human-readable display name of the model provider.", example = "My OpenAI Provider")
    private String name;

    @Schema(
            description = "Protocol type of the provider's API; determines how requests are formatted and sent.",
            example = "OPENAI_COMPATIBLE")
    private AgenticModelProviderTypeEnum providerType;

    @Schema(
            description = "Root URL of the provider's API endpoint; all model requests are sent relative to this base.",
            example = "https://api.openai.com/v1")
    private String baseUrl;

    @Schema(
            description =
                    "Indicates whether this provider is the tenant's default selection; only one provider per tenant may be DEFAULT at a time.",
            example = "NON_DEFAULT")
    private DefaultFlagEnum defaultFlag;

    @Schema(
            description = "Lifecycle state of this provider; DISABLE prevents it from being used in model calls.",
            example = "ENABLE")
    private EnableFlagEnum enableFlag;

    @Schema(
            description = "Identifier of the tenant that owns this provider; all operations are scoped to this tenant.",
            example = "1024")
    private String tenantId;

    @Schema(
            description =
                    "API key used to authenticate requests to the provider's endpoint. Write-only: never included in API responses.",
            accessMode = Schema.AccessMode.WRITE_ONLY)
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;

    @Schema(
            description =
                    "Status of the latest connectivity check against this provider; null when it has never been checked. Read-only: ignored on write.",
            example = "PASS")
    private ConnectivityStatusEnum lastCheckStatus;

    @Schema(
            description = "Completion time of the latest connectivity check. Read-only: ignored on write.",
            example = "2026-10-04T08:30:00")
    private LocalDateTime lastCheckTime;

    @Schema(
            description =
                    "Round-trip latency in milliseconds of the latest connectivity check. Read-only: ignored on write.",
            example = "320")
    private Integer lastCheckLatencyMs;

    @Schema(
            description =
                    "Error type code of the latest failed connectivity check, e.g. AUTH_FAILED; null when it passed. Read-only: ignored on write.",
            example = "AUTH_FAILED")
    private String lastCheckErrorType;

    @Schema(
            description =
                    "Sanitized error message of the latest failed connectivity check; never contains credentials. Read-only: ignored on write.",
            example = "HTTP 401 from api.example.com")
    private String lastCheckErrorMessage;

    @Schema(
            description =
                    "Model identifier the latest L2 chat probe used for this provider. Read-only: ignored on write.",
            example = "deepseek-chat")
    private String lastCheckModel;
}
