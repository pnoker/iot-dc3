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
 * A (source, eventType) pair that fired repeatedly in the window — i.e. is "flapping".
 * Different from Storm which is pure per-source volume; Flap narrows the signal to one
 * event-type so operators can tell whether the same condition keeps re-tripping.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "A source/event-type pair that fired repeatedly in the window (flapping)")
public class FlappingSourceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "alert source: device or driver")
    private String source;

    @Schema(description = "source entity ID")
    private long sourceId;

    @Schema(description = "Alarm type enum")
    private int alarmTypeFlag;

    @Schema(description = "number of times this source/type fired in the window")
    private long count;

}
