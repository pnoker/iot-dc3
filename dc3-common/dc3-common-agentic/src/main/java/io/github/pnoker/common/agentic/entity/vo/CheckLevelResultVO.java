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

import io.github.pnoker.common.enums.ConnectivityStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Single probe-level outcome (L1 or L2) of a connectivity check.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@Schema(description = "Outcome of one connectivity check probe level")
public class CheckLevelResultVO {

    @Schema(
            description = "Outcome of this probe level: PASS, FAIL, or SKIPPED when the level was not requested.",
            example = "PASS")
    private ConnectivityStatusEnum status;

    @Schema(
            description = "Round-trip latency of this probe in milliseconds; 0 when the probe failed before answering.",
            example = "320")
    private Long latencyMs;

    @Schema(
            description = "Error type code when this probe failed, e.g. AUTH_FAILED; null when it passed.",
            example = "AUTH_FAILED")
    private String errorType;

    @Schema(
            description =
                    "Sanitized upstream error message when this probe failed; never contains credentials. Null when it passed.",
            example = "HTTP 401 from api.example.com")
    private String message;

    @Schema(
            description = "HTTP status the upstream returned, when an HTTP response was received at all.",
            example = "401")
    private Integer upstreamStatus;

    @Schema(
            description =
                    "Model ids returned by the L1 model-list probe, truncated to the configured cap; L2 leaves this null.")
    private List<String> models;

    @Schema(description = "Model identifier the L2 chat probe used; L1 leaves this null.", example = "deepseek-chat")
    private String model;
}
