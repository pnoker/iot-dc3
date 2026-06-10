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
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Agentic Run Event response body")
public class AgenticRunEventResponse {

    @Schema(description = "Event object type")

    private String object;

    @Schema(description = "Event type")

    private String type;

    @Schema(description = "Event title")

    private String title;

    @Schema(description = "Event detail")

    private String detail;

    @Schema(description = "Event name")

    private String name;

    @Schema(description = "Run phase")

    private String phase;

    @Schema(description = "Run status")

    private String status;

    @Schema(description = "Event code")

    private String code;

    @Schema(description = "Unix timestamp when the event was created")

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
