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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * View object for agentic model API responses.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Model view object")
public class ModelVO {

    @Schema(description = "Unique model identifier used when invoking the AI provider API.", example = "gpt-4o")
    private String model;

    @Schema(description = "Human-readable display name of the model shown in the UI.", example = "GPT-4o")
    private String label;

    @Schema(description = "Whether the model supports streaming token-by-token responses via SSE.", example = "true")
    private boolean stream;

    @Schema(description = "Whether the model supports tool/function calling for structured action invocation.", example = "true")
    private boolean toolCall;

    @Schema(description = "Whether the model supports vision (image) input in addition to text.", example = "false")
    private boolean vision;

    @Schema(description = "Whether the model supports extended chain-of-thought reasoning before answering.", example = "false")
    private boolean reasoning;

    @Schema(description = "Sampling temperature controlling output randomness; higher values produce more creative responses. Valid range is typically 0.0–2.0.", example = "0.7")
    private Double temperature;

    @Schema(description = "Maximum number of tokens the model may generate in a single response. Capped by the model's context window limit.", example = "4096")
    private Integer maxTokens;

}
