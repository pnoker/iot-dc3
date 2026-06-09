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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Generic count bucket used by the manager dashboard breakdown endpoints: a string key
 * (status name, type name, or stringified id) paired with a row count. The frontend
 * resolves ids → names via existing batch APIs.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic count bucket: a string key paired with a row count")
public class BucketVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Human-readable bucket key (e.g. "ENABLE", "DISABLE", "GATEWAY", or an entity id
     * rendered as string).
     */
    @Schema(description = "Human-readable bucket key (status name, type name, or stringified entity id)", example = "ENABLE")
    private String key;

    @Schema(description = "Row count for this bucket", example = "12")
    private long count;

}
