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

import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for agentic model configuration API responses.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "Model configuration response, describing a single AI model and its capability flags within the current tenant.")
public class ModelConfigVO extends BaseVO {

    @Schema(description = "Model identifier string as used by the provider API (e.g. OpenAI model name).", example = "gpt-4o")
    private String model;

    @Schema(description = "Human-readable display name for the model, shown in the UI.", example = "GPT-4o")
    private String label;

    @Schema(description = "Identifier of the model provider this configuration belongs to; must belong to the current tenant.", example = "1024")
    private Long providerId;

    @Schema(description = "Display name of the model provider, denormalised for read convenience.", example = "OpenAI")
    private String providerName;

    @Schema(description = "Whether the model supports streaming (Server-Sent Events) responses.", example = "true")
    private Boolean stream;

    @Schema(description = "Whether the model supports tool/function calling.", example = "true")
    private Boolean toolCall;

    @Schema(description = "Whether the model supports vision (image) input.", example = "false")
    private Boolean vision;

    @Schema(description = "Whether the model supports chain-of-thought reasoning output.", example = "false")
    private Boolean reasoning;

    @Schema(description = "Sampling temperature controlling output randomness; typically in the range 0.0–2.0.", example = "0.7")
    private Double temperature;

    @Schema(description = "Maximum number of tokens the model will generate in a single response.", example = "4096")
    private Integer maxTokens;

    @Schema(description = "Whether this model is marked as the tenant default; only one model per tenant should be DEFAULT.", example = "NOT_DEFAULT")
    private DefaultFlagEnum defaultFlag;

    @Schema(description = "Enabled/disabled status of this model configuration within the tenant.", example = "ENABLE")
    private EnableFlagEnum enableFlag;

    @Schema(description = "Identifier of the tenant this model configuration belongs to.", example = "1024")
    private Long tenantId;

}
