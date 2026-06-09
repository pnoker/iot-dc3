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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Model Config view object")
public class ModelConfigVO extends BaseVO {

    @Schema(description = "Model identifier", example = "gpt-4o")
    private String model;

    @Schema(description = "Human-readable model label", example = "GPT-4o")
    private String label;

    @Schema(description = "Model provider ID", example = "1024")
    private Long providerId;

    @Schema(description = "Model provider name")
    private String providerName;

    @Schema(description = "Whether the model supports streaming responses")
    private Boolean stream;

    @Schema(description = "Whether the model supports tool/function calling")
    private Boolean toolCall;

    @Schema(description = "Whether the model supports vision (image) input")
    private Boolean vision;

    @Schema(description = "Whether the model supports reasoning")
    private Boolean reasoning;

    @Schema(description = "Sampling temperature", example = "0.7")
    private Double temperature;

    @Schema(description = "Maximum number of tokens to generate", example = "4096")
    private Integer maxTokens;

    @Schema(description = "Default flag", example = "DEFAULT")
    private DefaultFlagEnum defaultFlag;

    @Schema(description = "Enable flag", example = "ENABLE")
    private EnableFlagEnum enableFlag;

    @Schema(description = "Tenant ID", example = "1024")
    private Long tenantId;

}
