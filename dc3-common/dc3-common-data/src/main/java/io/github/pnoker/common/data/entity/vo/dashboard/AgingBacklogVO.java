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

/**
 * Count of still-unconfirmed alarms bucketed by how long they've been
 * sitting. The 24h+ bucket is the SLA breach indicator.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AgingBacklogVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private long under1h;
    private long h1to6;
    private long h6to24;
    private long over24h;
    /**
     * Convenience sum — equals the total unconfirmed count.
     */
    private long total;
}
