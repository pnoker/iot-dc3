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

package io.github.pnoker.common.driver.command;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceLockManagerTest {

    @Test
    void rejectsNullDeviceId() {
        DeviceLockManager manager = new DeviceLockManager();

        assertThatThrownBy(() -> manager.runExclusive(null, () -> {
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deviceId");
    }

    @Test
    void releasesLockAfterCommandCompletes() throws Exception {
        DeviceLockManager manager = new DeviceLockManager();
        AtomicInteger count = new AtomicInteger();

        manager.runExclusive(10L, count::incrementAndGet);

        assertThat(count.get()).isEqualTo(1);
        assertThat(lockMap(manager)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Map<Long, ?> lockMap(DeviceLockManager manager) throws Exception {
        Field field = DeviceLockManager.class.getDeclaredField("locks");
        field.setAccessible(true);
        return (Map<Long, ?>) field.get(manager);
    }
}
