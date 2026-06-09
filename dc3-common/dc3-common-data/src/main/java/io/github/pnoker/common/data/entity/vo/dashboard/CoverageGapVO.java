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
import java.util.ArrayList;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Config-vs-reality coverage report — points declared in dc3_point that never produced
 * any point_value row. {@code missingPoints / totalPoints} is the gap ratio;
 * {@code items} is the (capped) list of offending ids so the UI can drill in.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Config-vs-reality coverage report for declared points that never produced data")
public class CoverageGapVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "total number of declared points")
    private long totalPoints;

    @Schema(description = "number of declared points that never produced a value")
    private long missingPoints;

    @Schema(description = "capped list of offending point/profile ids")
    private List<Item> items = new ArrayList<>();

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @Schema(description = "One point with no recorded values")
    public static class Item implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "point ID")
        private long pointId;

        @Schema(description = "profile ID the point belongs to")
        private long profileId;

    }

}
