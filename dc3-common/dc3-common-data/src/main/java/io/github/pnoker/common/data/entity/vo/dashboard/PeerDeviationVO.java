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
 * A device whose alarm rate deviates sharply from its profile peers. The
 * ratio field is alarmCount / peerMedian; service filters to ratio &gt;= 3.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PeerDeviationVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private long profileId;
    private long deviceId;
    private long alarmCount;
    private long peerMedian;
    /**
     * alarmCount / peerMedian, 2-decimal. 0 means peerMedian was 0 (fresh profile).
     */
    private double ratio;
}
