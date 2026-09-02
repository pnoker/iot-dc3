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

import io.github.pnoker.common.enums.PointCommandSourceEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Business object for submitting a point read command.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
public class PointCommandReadBO {

    public PointCommandReadBO() {}

    public PointCommandReadBO(Long deviceId, Long pointId, String commandId) {
        this(deviceId, pointId, commandId, null);
    }

    public PointCommandReadBO(Long deviceId, Long pointId, String commandId, PointCommandSourceEnum source) {
        this.deviceId = deviceId;
        this.pointId = pointId;
        this.commandId = commandId;
        this.source = source;
    }

    /**
     * Device ID to read the point value from
     */
    private Long deviceId;

    /**
     * Data point ID to read
     */
    private Long pointId;

    /**
     * Optional pre-generated command ID for idempotent submission
     */
    private String commandId;

    private PointCommandSourceEnum source;
}
