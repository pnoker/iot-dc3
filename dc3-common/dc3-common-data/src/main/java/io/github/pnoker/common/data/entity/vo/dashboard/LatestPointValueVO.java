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

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One row in the dashboard live-data feed — the most recent N point-value
 * entries across every typed hypertable.
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LatestPointValueVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long deviceId;

    private Long pointId;

    private Long driverId;

    private String rawValue;

    private String calValue;

    /**
     * Which hypertable the row came from: STRING / INT / LONG / BOOL /
     * FLOAT / DOUBLE / JSON. Lets the UI pick formatting without fetching
     * the point's metadata.
     */
    private String valueType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
