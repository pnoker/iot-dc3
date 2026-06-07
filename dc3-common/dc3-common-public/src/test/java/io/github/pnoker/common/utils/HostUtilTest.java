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

package io.github.pnoker.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HostUtilTest {

    @Test
    void localHostReturnsAnAddressOrNull() {
        // The exact value depends on the test environment; we only assert that the
        // call does not throw and the result is either null (offline) or a non-blank
        // string that is a printable address.
        String host = HostUtil.localHost();
        if (host != null) {
            assertThat(host).isNotBlank();
        }
    }

    @Test
    void getHostNamesDoesNotThrowForUnknownAddress() {
        // DNS resolution of an unknown host is environment-dependent; some resolvers
        // may return NXDOMAIN (empty set) while others may redirect or resolve it.
        Set<String> names = HostUtil.getHostNames("not-a-real-host.invalid", true);
        assertThat(names).isNotNull();
    }

    @Test
    void getHostNamesIncludesLoopbackForLoopbackInput() {
        Set<String> names = HostUtil.getHostNames("127.0.0.1", true);
        assertThat(names).isNotEmpty();
    }

    @Test
    void getHostNamesExcludesLoopbackWhenAsked() {
        Set<String> names = HostUtil.getHostNames("127.0.0.1", false);
        // The loopback path is filtered out; resulting set is empty for a pure loopback
        // input.
        assertThat(names).isEmpty();
    }

    @Test
    void localMacListReturnsCollectionWithoutThrowing() {
        List<String> macs = HostUtil.localMacList();
        assertThat(macs).isNotNull();
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<HostUtil> constructor = HostUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
