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

package io.github.pnoker.common.entity.auth;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeysTest {

    @Test
    void aesAllArgsConstructorAssignsKey() {
        Keys.Aes aes = new Keys.Aes("private");
        assertThat(aes.getPrivateKey()).isEqualTo("private");
    }

    @Test
    void aesNoArgsConstructorLeavesKeyNull() {
        Keys.Aes aes = new Keys.Aes();
        assertThat(aes.getPrivateKey()).isNull();
        aes.setPrivateKey("set-later");
        assertThat(aes.getPrivateKey()).isEqualTo("set-later");
    }

    @Test
    void rsaAllArgsConstructorAssignsBothKeys() {
        Keys.Rsa rsa = new Keys.Rsa("pub", "priv");
        assertThat(rsa.getPublicKey()).isEqualTo("pub");
        assertThat(rsa.getPrivateKey()).isEqualTo("priv");
    }

    @Test
    void rsaSettersUpdateKeys() {
        Keys.Rsa rsa = new Keys.Rsa();
        rsa.setPublicKey("pub");
        rsa.setPrivateKey("priv");
        assertThat(rsa.getPublicKey()).isEqualTo("pub");
        assertThat(rsa.getPrivateKey()).isEqualTo("priv");
    }

    @Test
    void enclosingKeysClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<Keys> constructor = Keys.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
