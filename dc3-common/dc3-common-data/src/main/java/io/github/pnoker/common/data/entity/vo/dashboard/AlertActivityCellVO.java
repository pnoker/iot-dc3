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
 * One cell in the event-overview alarm heatmap (dow × hour). Service layer
 * always returns a fully-padded 7 × 24 grid.
 *
 * @author pnoker
 * @since 2026.5.3
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AlertActivityCellVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Day of week, 0..6 matching Postgres EXTRACT(DOW) (0 = Sunday).
     */
    private int dow;

    /**
     * Hour of day, 0..23.
     */
    private int hour;

    /**
     * Alarm count in that cell.
     */
    private long count;
}
