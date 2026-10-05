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
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Connectivity check outcome. The endpoint always answers HTTP 200 with this
 * object: a failed probe is a diagnosis, not a transport error.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@Schema(description = "Provider connectivity check result: L1/L2 probe outcomes and the dimensions they covered")
public class ProviderCheckResultVO {

    @Schema(description = "Overall verdict: PASS only when every requested probe level passed.", example = "FAIL")
    private ConnectivityStatusEnum overall;

    @Schema(description = "Outcome of the L1 model-list probe, or SKIPPED when only L2 was requested.")
    private CheckLevelResultVO l1;

    @Schema(description = "Outcome of the L2 minimal chat probe, or SKIPPED when only L1 was requested.")
    private CheckLevelResultVO l2;

    @Schema(
            description =
                    "Configuration dimensions this check actually verified: CONNECTIVITY, AUTH, MODEL_VISIBLE, INFERENCE.",
            example = "[\"CONNECTIVITY\", \"AUTH\", \"INFERENCE\"]")
    private List<String> dimensions;

    @Schema(description = "Completion time of this check.", example = "2026-10-04T08:30:00")
    private LocalDateTime checkedAt;
}
