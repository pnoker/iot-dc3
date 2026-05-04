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
import java.util.ArrayList;
import java.util.List;

/**
 * Payload for GET /manager/dashboard/topology — a 4-column Sankey graph
 * (Driver → Device → Profile → Point) the home page uses to show how the
 * tenant's metadata and data flow wire together.
 *
 * <p>Top-N cropping happens server-side so large tenants don't blow up the
 * payload. Whatever was cropped rolls up into {@code others:*} nodes whose
 * {@link TopologyNodeVO#getHiddenChildren()} holds the real list for a
 * drill-in dialog.</p>
 *
 * @author pnoker
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
public class TopologyVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<TopologyNodeVO> nodes = new ArrayList<>();
    private List<TopologyLinkVO> links = new ArrayList<>();
    private TopologyStatsVO stats = new TopologyStatsVO();
}
