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

import io.github.pnoker.common.enums.EntityStatusEnum;
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
 * <p>{@code timeout} and {@code timeoutUnit} are optional. When a driver leaves them
 * empty, the SDK uses {@code dc3.driver.health.device.timeout} and
 * {@code dc3.driver.health.device.timeout-unit} as the fallback
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
    private EntityStatusEnum status = EntityStatusEnum.ONLINE;

    /**
     * Per-device lease timeout value. {@code null} means use SDK fallback config.
     */
    private Integer timeout;

    /**
     * Per-device lease timeout unit. {@code null} means using the configured fallback unit.
     */
    private TimeUnit timeoutUnit;

    /**
     * Structured description for diagnostics (e.g. connectivity state, error details).
     * Stored in {@code dc3_entity_state.entity_state_ext.content}.
     */
    private String description;

    public static DeviceHealthState online() {
        return of(EntityStatusEnum.ONLINE, null, null);
    }

    public static DeviceHealthState online(int timeout, TimeUnit timeoutUnit) {
        return of(EntityStatusEnum.ONLINE, timeout, timeoutUnit);
    }

    public static DeviceHealthState offline() {
        return of(EntityStatusEnum.OFFLINE, null, null);
    }

    public static DeviceHealthState offline(int timeout, TimeUnit timeoutUnit) {
        return of(EntityStatusEnum.OFFLINE, timeout, timeoutUnit);
    }

    public static DeviceHealthState maintain() {
        return of(EntityStatusEnum.MAINTAIN, null, null);
    }

    public static DeviceHealthState maintain(int timeout, TimeUnit timeoutUnit) {
        return of(EntityStatusEnum.MAINTAIN, timeout, timeoutUnit);
    }

    public static DeviceHealthState fault() {
        return of(EntityStatusEnum.FAULT, null, null);
    }

    public static DeviceHealthState fault(int timeout, TimeUnit timeoutUnit) {
        return of(EntityStatusEnum.FAULT, timeout, timeoutUnit);
    }

    public static DeviceHealthState of(EntityStatusEnum status, Integer timeout, TimeUnit timeoutUnit) {
        return DeviceHealthState.builder()
                .status(status)
                .timeout(timeout)
                .timeoutUnit(timeoutUnit)
                .build();
    }

}
