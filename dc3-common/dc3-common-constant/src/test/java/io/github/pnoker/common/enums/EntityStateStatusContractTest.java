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

package io.github.pnoker.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract for status enums backed by {@code dc3_entity_state.state_flag}.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
class EntityStateStatusContractTest {

    @Test
    void driverAndDeviceStateStatusesStayAligned() {
        assertThat(DriverStatusEnum.values()).hasSameSizeAs(DeviceStatusEnum.values());
        for (DriverStatusEnum driverStatus : DriverStatusEnum.values()) {
            DeviceStatusEnum deviceStatus = DeviceStatusEnum.valueOf(driverStatus.name());
            assertThat(deviceStatus.getIndex()).as(driverStatus.name() + " index").isEqualTo(driverStatus.getIndex());
            assertThat(deviceStatus.getCode()).as(driverStatus.name() + " code").isEqualTo(driverStatus.getCode());
        }
    }

}
