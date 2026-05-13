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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegexUtilTest {

    @ParameterizedTest
    @CsvSource({
            "0,        true",
            "0.1,      true",
            "-1.5,     true",
            "1e10,     true",
            "1.2.3,    false",
            "abc,      false"
    })
    void isNumericMatchesBigDecimalParseability(String input, boolean expected) {
        assertThat(RegexUtil.isNumeric(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void isNumericRejectsNullAndEmpty(String input) {
        assertThat(RegexUtil.isNumeric(input)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice", "iot-dc3", "device_01", "设备/A.1"})
    void isNameAcceptsValidNames(String name) {
        assertThat(RegexUtil.isName(name)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "_starts-with-underscore", "  spaced"})
    void isNameRejectsInvalidNames(String name) {
        assertThat(RegexUtil.isName(name)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"13800138000", "15912345678", "19987654321"})
    void isPhoneAcceptsChineseMobile(String phone) {
        assertThat(RegexUtil.isPhone(phone)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "23800138000", "+8613800138000"})
    void isPhoneRejectsInvalidPhone(String phone) {
        assertThat(RegexUtil.isPhone(phone)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo@example.com", "alice.bob@a.io", "x_y@x.org"})
    void isMailAcceptsValidAddresses(String mail) {
        assertThat(RegexUtil.isMail(mail)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-at-sign", "@no-local.com", "no-domain@", "no-tld@host"})
    void isMailRejectsInvalidAddresses(String mail) {
        assertThat(RegexUtil.isMail(mail)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdefgh", "Pass1234", "longer123456789"})
    void isPasswordAcceptsValidPasswords(String password) {
        assertThat(RegexUtil.isPassword(password)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "1starts-with-digit-and-too-long-and-too-long"})
    void isPasswordRejectsInvalidPasswords(String password) {
        assertThat(RegexUtil.isPassword(password)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "192.168.1.1", "10.0.0.255"})
    void isHostAcceptsIpv4(String host) {
        assertThat(RegexUtil.isHost(host)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"256.0.0.1", "1.2.3", "::1", "host.example.com"})
    void isHostRejectsInvalidHosts(String host) {
        assertThat(RegexUtil.isHost(host)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "8600, true",
            "8700, true",
            "8799, true",
            "8599, false",
            "8800, false",
            "0,    false"
    })
    void isDriverPortChecksRange(int port, boolean expected) {
        assertThat(RegexUtil.isDriverPort(port)).isEqualTo(expected);
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<RegexUtil> constructor = RegexUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
