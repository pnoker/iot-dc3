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
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import io.github.pnoker.common.enums.ConnectivityCheckLevelEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Request for a provider connectivity check. Two input modes: with {@code id}
 * the saved provider is loaded first and missing fields fall back to its
 * stored values (an absent or empty {@code apiKey} uses the stored key);
 * without {@code id} the request body is a draft configuration that is probed
 * without being persisted.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@Schema(
        description =
                "Provider connectivity check request: probe a saved provider by id or an unsaved draft configuration")
public class ProviderCheckRequestVO {

    @Schema(
            description =
                    "Identifier of a saved provider to check. When present, fields left blank fall back to its stored values.",
            example = "1024")
    private String id;

    @Schema(
            description =
                    "Protocol type of the provider; required for draft checks without an id. Ignored when an id is given and the field is blank.",
            example = "OPENAI_COMPATIBLE")
    private AgenticModelProviderTypeEnum providerType;

    @Schema(
            description =
                    "Root URL of the provider API; required for draft checks without an id. Falls back to the stored value when an id is given and this is blank.",
            example = "https://api.deepseek.com")
    private String baseUrl;

    @Schema(
            description =
                    "API key for the probe. Write-only: never included in API responses. Blank with an id present means: use the stored key; an explicit empty string is a deliberate no-key probe (legal for local endpoints).",
            accessMode = Schema.AccessMode.WRITE_ONLY)
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;

    @Schema(
            description =
                    "Model identifier the L2 chat probe should use. Blank picks the provider's first enabled model config, then the first model from the L1 list.",
            example = "deepseek-chat")
    private String model;

    @Schema(
            description = "Probe level: L1 model-list only, L2 minimal chat only, or BOTH (default when blank).",
            example = "BOTH")
    private ConnectivityCheckLevelEnum level;
}
