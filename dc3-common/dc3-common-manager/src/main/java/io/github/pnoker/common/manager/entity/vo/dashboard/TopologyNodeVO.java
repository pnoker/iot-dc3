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
import java.util.List;

/**
 * One node in the topology Sankey. {@code id} is a prefixed string so the
 * frontend can route on type + key in one hop (e.g. {@code "driver:42"}
 * → {@code driverDetail/42}; {@code "others:point:51"} → dialog for
 * profile 51's cropped points).
 *
 * @author pnoker
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
public class TopologyNodeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Prefixed id — one of {@code driver:{n}}, {@code device:{n}}, {@code profile:{n}}, {@code point:{n}}, {@code others:{layer}:{parentId}}.
     */
    private String id;

    /**
     * Human-readable label. For {@code others:*} nodes, {@code "Others (N)"} where N is the count of hidden children.
     */
    private String name;

    /**
     * 1 = driver, 2 = device, 3 = profile, 4 = point. Frontend fixes column x-position from this.
     */
    private int layer;

    /**
     * {@code driver | device | profile | point | others}. Drives node colour + click routing.
     */
    private String type;

    /**
     * Only populated on {@code others:*} nodes. List of the actual entities collapsed here — frontend shows these in a drill-in dialog.
     */
    private List<TopologyHiddenChildVO> hiddenChildren;
}
