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
package io.github.pnoker.common.driver.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DriverMetadataLeaseTest {

    @Test
    void exposesOwnedDevicesOnlyWhileLeaseIsValid() {
        DriverMetadata metadata = new DriverMetadata();
        metadata.setDeviceLeases(Map.of(10L, 77L), System.currentTimeMillis() + 10_000, 5L);

        assertThat(metadata.getDeviceIds()).containsExactly(10L);
        assertThat(metadata.getFencingToken(10L)).isEqualTo(77L);
        assertThat(metadata.getAssignmentVersion()).isEqualTo(5L);

        metadata.renewLeaseDeadline(System.currentTimeMillis() - 1);

        assertThat(metadata.getDeviceIds()).isEmpty();
        assertThat(metadata.getFencingToken(10L)).isNull();
        assertThat(metadata.getAssignmentVersion()).isEqualTo(5L);
    }
}
