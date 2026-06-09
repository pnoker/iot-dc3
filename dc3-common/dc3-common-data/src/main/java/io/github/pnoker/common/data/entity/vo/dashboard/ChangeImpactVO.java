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
 * One config-change event — a driver/device/profile row whose operate_time differs from
 * create_time (i.e. someone edited it). Overlaid on the alarm trend chart so spikes can
 * be attributed to recent changes.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "One config-change event overlaid on the alarm trend chart")
public class ChangeImpactVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * {@code driver} | {@code device} | {@code profile}.
     */
    @Schema(description = "changed entity kind: driver, device or profile")
    private String kind;

    @Schema(description = "changed entity ID")
    private long entityId;

    @Schema(description = "time the entity was edited")
    private LocalDateTime operateTime;

}
