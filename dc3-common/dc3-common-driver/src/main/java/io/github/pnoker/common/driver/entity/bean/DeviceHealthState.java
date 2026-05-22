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

package io.github.pnoker.common.driver.entity.bean;

import io.github.pnoker.common.enums.DeviceStatusEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Device health result returned by protocol drivers.
 *
 * <p>{@code timeout} and {@code timeUnit} are optional. When a driver leaves them
 * empty, the SDK uses {@code dc3.driver.health.device.timeout-seconds} as the fallback
 * device state lease.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceHealthState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Device state that should be reported.
     */
    @Builder.Default
    private DeviceStatusEnum status = DeviceStatusEnum.ONLINE;

    /**
     * Per-device lease timeout value. {@code null} means use SDK fallback config.
     */
    private Integer timeout;

    /**
     * Per-device lease timeout unit. {@code null} means seconds.
     */
    private TimeUnit timeUnit;

    public static DeviceHealthState online() {
        return of(DeviceStatusEnum.ONLINE, null, null);
    }

    public static DeviceHealthState online(int timeout, TimeUnit timeUnit) {
        return of(DeviceStatusEnum.ONLINE, timeout, timeUnit);
    }

    public static DeviceHealthState offline() {
        return of(DeviceStatusEnum.OFFLINE, null, null);
    }

    public static DeviceHealthState offline(int timeout, TimeUnit timeUnit) {
        return of(DeviceStatusEnum.OFFLINE, timeout, timeUnit);
    }

    public static DeviceHealthState of(DeviceStatusEnum status, Integer timeout, TimeUnit timeUnit) {
        return DeviceHealthState.builder()
                .status(status)
                .timeout(timeout)
                .timeUnit(timeUnit)
                .build();
    }

}
