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

/**
 * A device whose alarm rate deviates sharply from its profile peers. The ratio field is
 * alarmCount / peerMedian; service filters to ratio &gt;= 3.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PeerDeviationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long profileId;

    private long deviceId;

    private long alarmCount;

    private long peerMedian;

    /**
     * alarmCount / peerMedian, 2-decimal. 0 means peerMedian was 0 (fresh profile).
     */
    private double ratio;

}
