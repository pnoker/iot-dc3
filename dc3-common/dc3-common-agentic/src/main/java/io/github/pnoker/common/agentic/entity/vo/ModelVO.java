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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(description = "model")

    private String model;

    @Schema(description = "label")

    private String label;

    @Schema(description = "stream")

    private boolean stream;

    @Schema(description = "tool call")

    private boolean toolCall;

    @Schema(description = "vision")

    private boolean vision;

    @Schema(description = "reasoning")

    private boolean reasoning;

    @Schema(description = "temperature")

    private Double temperature;

    @Schema(description = "max tokens")

    private Integer maxTokens;

}
