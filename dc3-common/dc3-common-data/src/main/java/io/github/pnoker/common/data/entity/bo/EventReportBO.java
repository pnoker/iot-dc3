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

package io.github.pnoker.common.data.entity.bo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Business object for reporting an event from a device or external system.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EventReportBO {

    /**
     * Device ID
     */
    private Long deviceId;

    /**
     * Event ID reported from the device
     */
    private Long eventId;

    /**
     * Event code, stable business identifier matching the event definition
     */
    private String eventCode;

    /**
     * Parameter values submitted with this event report, keyed by parameter code
     */
    private Map<String, String> paramValues;

    /**
     * Message content
     */
    private String message;

}
