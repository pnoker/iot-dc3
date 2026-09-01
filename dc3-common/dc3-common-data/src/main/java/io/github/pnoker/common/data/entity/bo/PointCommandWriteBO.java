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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import io.github.pnoker.common.enums.PointCommandSourceEnum;

/**
 * Business object for submitting a point write command.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
public class PointCommandWriteBO {

    public PointCommandWriteBO() {
    }

    public PointCommandWriteBO(Long deviceId, Long pointId, String value, String commandId) {
        this(deviceId, pointId, value, commandId, null);
    }

    public PointCommandWriteBO(Long deviceId, Long pointId, String value, String commandId,
                               PointCommandSourceEnum source) {
        this.deviceId = deviceId;
        this.pointId = pointId;
        this.value = value;
        this.commandId = commandId;
        this.source = source;
    }

    /**
     * Device ID to write the point value to
     */
    private Long deviceId;

    /**
     * Data point ID to write to
     */
    private Long pointId;

    /**
     * Value to write to the data point
     */
    private String value;

    /**
     * Optional pre-generated command ID for idempotent submission
     */
    private String commandId;

    private PointCommandSourceEnum source;

}
