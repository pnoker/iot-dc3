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
 * Count of still-unconfirmed alarms bucketed by how long they've been sitting. The 24h+
 * bucket is the SLA breach indicator.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Unconfirmed alarm aging backlog buckets")
public class AgingBacklogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "count of unconfirmed alarms aged under 1 hour")
    private long under1h;

    @Schema(description = "count of unconfirmed alarms aged 1 to 6 hours")
    private long h1to6;

    @Schema(description = "count of unconfirmed alarms aged 6 to 24 hours")
    private long h6to24;

    @Schema(description = "count of unconfirmed alarms aged over 24 hours (SLA breach indicator)")
    private long over24h;

    /**
     * Convenience sum — equals the total unconfirmed count.
     */
    @Schema(description = "convenience sum, equals the total unconfirmed count")
    private long total;

}
