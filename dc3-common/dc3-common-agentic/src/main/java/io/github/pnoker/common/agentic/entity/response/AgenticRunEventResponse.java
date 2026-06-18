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
import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Structured SSE payload for one agentic runtime event.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Structured SSE payload describing a single agentic runtime event; streamed to the client during an agentic turn.")
public class AgenticRunEventResponse {

    @Schema(description = "Fixed object type discriminator for agentic run events; always \"agentic.event\".", example = "agentic.event")

    private String object;

    @Schema(description = "Category of the event; one of: event, tool, reasoning, error.", example = "tool")

    private String type;

    @Schema(description = "Human-readable headline summarising what happened in this event.", example = "Query device history")

    private String title;

    @Schema(description = "Additional detail or domain context for the event, such as the tool domain or error message.", example = "Querying point history data")

    private String detail;

    @Schema(description = "Name of the tool or agent component that produced this event.", example = "queryPointHistory")

    private String name;

    @Schema(description = "Lifecycle phase of the event within its operation; one of: start, result, error.", example = "start")

    private String phase;

    @Schema(description = "Execution status of the event; one of: running, success, empty, failed.", example = "running")

    private String status;

    @Schema(description = "Machine-readable result code returned by the tool; one of: OK, EMPTY, INVALID_ARGUMENT, NOT_FOUND, UNAVAILABLE, ERROR.", example = "OK")

    private String code;

    @Schema(description = "Unix epoch seconds when the event was emitted; derived from the internal millisecond timestamp divided by 1000.", example = "1718700000")

    private Long created;

    public static AgenticRunEventResponse of(AgenticRunEvent runEvent) {
        AgenticRunEventResponse response = new AgenticRunEventResponse();
        response.setObject(AgenticConstant.RunEvent.OBJECT);
        response.setType(runEvent.type());
        response.setTitle(runEvent.title());
        response.setDetail(runEvent.detail());
        response.setName(runEvent.name());
        response.setPhase(StringUtils.trimToNull(runEvent.phase()));
        response.setStatus(StringUtils.trimToNull(runEvent.status()));
        response.setCode(StringUtils.trimToNull(runEvent.code()));
        response.setCreated(runEvent.timestamp() / 1000);
        return response;
    }

}
