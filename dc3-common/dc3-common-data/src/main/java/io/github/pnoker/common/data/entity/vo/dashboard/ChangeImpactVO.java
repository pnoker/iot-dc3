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
 * One config-change event — a driver/device/profile row whose operate_time
 * differs from create_time (i.e. someone edited it). Overlaid on the
 * alarm trend chart so spikes can be attributed to recent changes.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ChangeImpactVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * {@code driver} | {@code device} | {@code profile}.
     */
    private String kind;
    private long entityId;
    private LocalDateTime operateTime;
}
