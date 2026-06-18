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
package io.github.pnoker.common.agentic.entity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Session extension metadata persisted as JSON.
 *
 * @author pnoker
 * @version 2026.5.15
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Session extension metadata persisted as JSON")
public class SessionExt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Identifier of the AI model used for this session (e.g. provider model name).", example = "gpt-4o")
    private String model;

    @Schema(description = "Whether chain-of-thought / extended reasoning capability is enabled for this session.", example = "false")
    private Boolean reasoningEnabled;

    @Schema(description = "Sampling temperature controlling response randomness; higher values increase diversity (typically 0.0–2.0).", example = "0.7")
    private Double temperature;

    @Schema(description = "Maximum number of tokens the model may generate in a single response.", example = "2048")
    private Integer maxTokens;

}
