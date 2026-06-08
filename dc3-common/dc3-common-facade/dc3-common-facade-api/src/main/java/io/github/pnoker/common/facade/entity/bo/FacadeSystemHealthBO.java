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

package io.github.pnoker.common.facade.entity.bo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Transport-neutral system health snapshot.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "Facade System Health business object")
public class FacadeSystemHealthBO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Map<String, String> center;

    private Map<String, String> infra;

    @Schema(description = "drivers")

    private FleetSummary drivers;

    @Schema(description = "devices")

    private FleetSummary devices;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class FleetSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Total record count")

        private int total;

        @Schema(description = "online")

        private int online;

    }

}
