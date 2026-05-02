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

package io.github.pnoker.common.data.entity.vo.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One row in the alert list panel on the home page. Source is either
 * {@code device} (with point_id) or {@code driver}.
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AlertItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String source;

    private Long sourceId;

    private Long pointId;

    private Integer eventTypeFlag;

    private Integer confirmFlag;

    private LocalDateTime createTime;

    /**
     * Human-readable message extracted from event_ext->>'content'.
     * Populated by the paging / list endpoints; latest(size=N) leaves it null.
     */
    private String message;
}
