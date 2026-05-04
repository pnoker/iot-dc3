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
 * One edge in the Sankey. In cardinality mode {@code value} is the number
 * of relationships this edge represents (1 per device for Driver→Device,
 * 1 per profile_bind row for Device→Profile, 1 per point for Profile→Point;
 * aggregated into N for {@code *→others} edges). In volume mode (Phase 2)
 * it carries the point_value sample count rolled up along the edge.
 *
 * @author pnoker
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
public class TopologyLinkVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String source;
    private String target;
    private long value;
}
