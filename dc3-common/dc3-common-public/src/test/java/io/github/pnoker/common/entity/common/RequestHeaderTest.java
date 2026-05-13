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

package io.github.pnoker.common.entity.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestHeaderTest {

    @Test
    void tokenHeaderConstructsAndExposesFields() {
        RequestHeader.TokenHeader token = new RequestHeader.TokenHeader("salty", "abc.def.ghi");
        assertThat(token.getSalt()).isEqualTo("salty");
        assertThat(token.getToken()).isEqualTo("abc.def.ghi");
    }

    @Test
    void tokenHeaderSettersUpdateFields() {
        RequestHeader.TokenHeader token = new RequestHeader.TokenHeader();
        token.setSalt("s");
        token.setToken("t");
        assertThat(token.getSalt()).isEqualTo("s");
        assertThat(token.getToken()).isEqualTo("t");
    }

    @Test
    void userHeaderConstructsAndExposesFields() {
        RequestHeader.UserHeader user = new RequestHeader.UserHeader(7L, "Alice", "alice", 1L);
        assertThat(user.getUserId()).isEqualTo(7L);
        assertThat(user.getNickName()).isEqualTo("Alice");
        assertThat(user.getUserName()).isEqualTo("alice");
        assertThat(user.getTenantId()).isEqualTo(1L);
    }

    @Test
    void enclosingRequestHeaderConstructorMustReject() throws NoSuchMethodException {
        Constructor<RequestHeader> constructor = RequestHeader.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
