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

package io.github.pnoker.common.entity.common;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Pagination parameters for data queries.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */

@Getter
@Setter
@ToString
@Schema(description = "Pagination parameters for data queries")
public class Pages implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Current page number, starting from 1; ignored when count is disabled", example = "1")
    private long current = 1;

    @Schema(description = "Number of records per page; clamped to the configured page-size limit", example = "10")
    private long size = DefaultConstant.PAGE_SIZE;

    @Schema(description = "Inclusive start timestamp (epoch milliseconds) for time-range filtering", example = "1717200000000")
    private long startTime;

    @Schema(description = "Inclusive end timestamp (epoch milliseconds) for time-range filtering", example = "1717286400000")
    private long endTime;

    @Schema(description = "Sort order items applied to the query results")
    private List<OrderItem> orders = new ArrayList<>(2);

}
