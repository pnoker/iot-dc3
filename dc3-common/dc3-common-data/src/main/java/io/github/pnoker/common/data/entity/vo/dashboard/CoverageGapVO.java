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
import java.util.ArrayList;
import java.util.List;

/**
 * Config-vs-reality coverage report — points declared in dc3_point that
 * never produced any point_value row. {@code missingPoints / totalPoints}
 * is the gap ratio; {@code items} is the (capped) list of offending ids
 * so the UI can drill in.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class CoverageGapVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private long totalPoints;
    private long missingPoints;
    private List<Item> items = new ArrayList<>();

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private long pointId;
        private long profileId;
    }
}
