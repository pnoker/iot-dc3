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

package io.github.pnoker.common.manager.entity.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Daily new-row counts for each stat-card entity, fixed length = days. Used by the home
 * page sparklines. Zero-padded missing days, oldest first.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Daily new-row counts per entity for the home page sparklines (zero-padded, oldest first)")
public class GrowthVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Daily new-driver counts, oldest first")
    private List<Long> driverDailyCounts;

    @Schema(description = "Daily new-device counts, oldest first")
    private List<Long> deviceDailyCounts;

    @Schema(description = "Daily new-point counts, oldest first")
    private List<Long> pointDailyCounts;

    @Schema(description = "Daily new-profile counts, oldest first")
    private List<Long> profileDailyCounts;

}
