/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.data.entity.vo.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One bin of the acquisition-to-storage latency histogram. The {@code bin} index is
 * mapped to a human-readable label by the frontend: 0=&lt;100ms, 1=100-500ms, 2=500ms-1s,
 * 3=1-5s, 4=5-30s, 5=&gt;=30s.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "One bin of the acquisition-to-storage latency histogram")
public class LatencyBucketVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "latency bin index: 0=<100ms, 1=100-500ms, 2=500ms-1s, 3=1-5s, 4=5-30s, 5=>=30s")
    private int bin;

    @Schema(description = "sample count in this latency bin")
    private long count;

}
