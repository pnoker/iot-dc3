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
package io.github.pnoker.common.agentic.entity.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.agentic.entity.model.AgenticVisualizationSpec;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Structured SSE payload for one agentic visualization.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "SSE payload returned by the agentic engine when a visualization chart has been produced; wraps a visualization specification together with a creation timestamp.")
public class AgenticVisualizationResponse {

    @Schema(description = "Fixed discriminator identifying this SSE payload as a visualization object; always \"agentic.visualization\".", example = "agentic.visualization")

    private String object;

    @Schema(description = "Structured visualization specification carrying chart type, data, and rendering options.")

    private AgenticVisualizationSpec visualization;

    @Schema(description = "Unix timestamp (milliseconds since epoch) when the visualization response was generated.", example = "1718678400000")

    private Long created;

    public static AgenticVisualizationResponse of(AgenticVisualizationSpec visualization, long created) {
        AgenticVisualizationResponse response = new AgenticVisualizationResponse();
        response.setObject(AgenticConstant.Visualization.OBJECT);
        response.setVisualization(visualization);
        response.setCreated(created);
        return response;
    }

}
