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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.agentic.entity.model.SessionExt;
import io.github.pnoker.common.entity.base.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for session API responses.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Session view object")
public class SessionVO extends BaseVO {

    @Schema(description = "Unique identifier of the conversation this session belongs to.", example = "conv-20240618-001")
    private String conversationId;

    @Schema(description = "Human-readable title summarising the session topic.", example = "Device diagnostics Q&A")
    private String title;

    @Schema(description = "Extended attributes of the session (model configuration, temperature, context window, etc.); structured as a JSON object.")
    private SessionExt sessionExt;

    @Schema(description = "Identifier of the tenant that owns this session; enforces tenant-level data isolation.", example = "1024")
    private Long tenantId;

    @Schema(description = "Identifier of the user who created or owns this session; must belong to the same tenant.", example = "2048")
    private Long userId;

}
