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
 * Two events that fired within a small time window of each other, enough
 * times to suggest a cascading-failure relationship. Frontend renders
 * these as a network graph (A—B edge weighted by coCount).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class CorrelationPairVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String aSource;
    private long aSourceId;
    private int aEventType;
    private String bSource;
    private long bSourceId;
    private int bEventType;
    private long coCount;
}
