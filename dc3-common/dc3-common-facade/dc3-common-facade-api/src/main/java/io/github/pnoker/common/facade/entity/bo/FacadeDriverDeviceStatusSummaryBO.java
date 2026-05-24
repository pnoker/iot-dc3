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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Device online/offline summary under one driver.
 *
 * @author pnoker
 * @version 2026.5.18
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FacadeDriverDeviceStatusSummaryBO implements Serializable {

    public static final String DRIVER_ID = "driverId";
    public static final String TOTAL = "total";
    public static final String ONLINE = "ONLINE";
    public static final String OFFLINE = "OFFLINE";
    @Serial
    private static final long serialVersionUID = 1L;
    private Long driverId;

    private int total;

    private int online;

    private int offline;

    public static FacadeDriverDeviceStatusSummaryBO fromMap(Map<String, String> source) {
        if (Objects.isNull(source) || source.isEmpty()) {
            return null;
        }
        return new FacadeDriverDeviceStatusSummaryBO(
                parseLong(source.get(DRIVER_ID)),
                parseInt(source.get(TOTAL)),
                parseInt(source.get(ONLINE)),
                parseInt(source.get(OFFLINE)));
    }

    private static Long parseLong(String value) {
        if (Objects.isNull(value) || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private static int parseInt(String value) {
        if (Objects.isNull(value) || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    public Map<String, String> toMap() {
        Map<String, String> target = new LinkedHashMap<>();
        target.put(DRIVER_ID, Objects.toString(driverId, ""));
        target.put(TOTAL, String.valueOf(total));
        target.put(ONLINE, String.valueOf(online));
        target.put(OFFLINE, String.valueOf(offline));
        return target;
    }

}
