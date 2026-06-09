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
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A (device, point) pair that was active within the baseline window but has gone silent
 * for at least {@code silentMinutes}. The absence of data is itself a signal — sensors
 * don't self-report "I'm offline".
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "A device/point pair that was active but has gone silent")
public class SilentSourceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "device ID")
    private long deviceId;

    @Schema(description = "point ID")
    private long pointId;

    @Schema(description = "time the last sample was seen")
    private LocalDateTime lastSeen;

    /**
     * How many seconds since the last sample, rounded.
     */
    @Schema(description = "seconds since the last sample, rounded")
    private long silentSeconds;

}
