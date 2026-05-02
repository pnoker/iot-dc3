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
 * One cell of the day-of-week × hour-of-day activity heatmap.
 * {@code dow} is 0=Sunday..6=Saturday; {@code hour} is 0..23.
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ActivityCellVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int dow;

    private int hour;

    private long count;
}
