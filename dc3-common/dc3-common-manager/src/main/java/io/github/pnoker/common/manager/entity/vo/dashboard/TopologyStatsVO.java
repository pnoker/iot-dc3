/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.manager.entity.vo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Total counts across the tenant (not just the Top-N rendered in the
 * Sankey). Frontend uses this for the footer summary bar — "X Driver ·
 * Y Device · Z Profile · W Point" — so the user can see how much the
 * diagram has been cropped.
 *
 * @author pnoker
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
public class TopologyStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long driverCount;
    private long deviceCount;
    private long profileCount;
    private long pointCount;
    /**
     * Short human-readable range label ("24h" / "7d" / ...) — present only
     * when the server computed volumes over a time window (volume mode).
     * null for cardinality mode so the frontend footer can skip it.
     */
    private String rangeLabel;
}
