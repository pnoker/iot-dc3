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

package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.biz.TokenService;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenLocalFacadeTest {

    @Mock
    private TokenService tokenService;

    private TokenLocalFacade facade;

    @BeforeEach
    void setUp() {
        facade = new TokenLocalFacade(tokenService);
    }

    @Test
    void checkValidReturnsFalseWhenServiceReturnsNull() {
        when(tokenService.checkValid("alice", "salt", "token", "tenant-A")).thenReturn(null);
        assertThat(facade.checkValid("tenant-A", "alice", "salt", "token")).isFalse();
    }

    @Test
    void checkValidReturnsFalseWhenTokenInvalid() {
        TokenValid invalid = new TokenValid();
        invalid.setValid(false);
        when(tokenService.checkValid("alice", "salt", "token", "tenant-A")).thenReturn(invalid);
        assertThat(facade.checkValid("tenant-A", "alice", "salt", "token")).isFalse();
    }

    @Test
    void checkValidReturnsTrueWhenTokenValid() {
        TokenValid valid = new TokenValid();
        valid.setValid(true);
        when(tokenService.checkValid("alice", "salt", "token", "tenant-A")).thenReturn(valid);
        assertThat(facade.checkValid("tenant-A", "alice", "salt", "token")).isTrue();
    }
}
