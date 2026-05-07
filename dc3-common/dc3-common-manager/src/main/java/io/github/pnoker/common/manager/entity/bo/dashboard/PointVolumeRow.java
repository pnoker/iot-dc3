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

package io.github.pnoker.common.manager.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Volume aggregate row for topology's volume mode — one (device_id, point_id) pair + its
 * pv sample count over the query window.
 */
@Getter
@Setter
@ToString
public class PointVolumeRow {

    private long deviceId;

    private long pointId;

    private long cnt;

}
