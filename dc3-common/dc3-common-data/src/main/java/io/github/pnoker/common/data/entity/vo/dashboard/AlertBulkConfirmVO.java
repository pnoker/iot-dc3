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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

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
public class AlertBulkConfirmVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Direction of the bulk operation: true sets confirm_flag to 1, false sets it back
     * to 0. Defaults to true when null.
     */
    @Schema(description = "Direction of the bulk operation: true sets confirm_flag to 1 (confirmed), false resets it to 0 (unconfirmed). Defaults to true when null.", example = "true")
    private Boolean confirm;

    /**
     * Targets to act on.
     */
    @Schema(description = "Alert targets to apply the bulk operation to; each entry identifies a single alert row in the current tenant scope.")
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
        @Schema(description = "Alert source classification: \"device\" for device-originated alerts or \"driver\" for driver-originated alerts; must match a value stored in dc3_entity_alert.source.", example = "device")
        private String source;

        /**
         * Alert row id.
         */
        @Schema(description = "Primary key of the alert row in dc3_entity_alert to confirm or unconfirm.", example = "1024")
        private Long id;

    }

}
