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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bulk confirm/unconfirm request body for the dashboard alert panel.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk confirm/unconfirm request for the dashboard alert panel")
public class AlertBulkConfirmRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Direction of the bulk operation: true sets confirm_flag to 1, false sets it back
     * to 0. Defaults to true when null.
     */
    @Schema(description = "direction of the bulk operation: true confirms, false unconfirms; defaults to true when null")
    private Boolean confirm;

    /**
     * Targets to act on.
     */
    @Schema(description = "targets to act on")
    private List<Item> items;

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Single alert target for the bulk operation")
    public static class Item implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Alert source: {@code device} or {@code driver}.
         */
        @Schema(description = "alert source: device or driver")
        private String source;

        /**
         * Alert row id.
         */
        @Schema(description = "alert row ID")
        private Long id;

    }

}
