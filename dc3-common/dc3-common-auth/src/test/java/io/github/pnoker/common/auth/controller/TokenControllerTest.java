/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.auth.controller;

import io.github.pnoker.common.auth.biz.ReactiveTokenService;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.auth.entity.query.TokenQuery;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.exception.UnAuthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import reactor.test.StepVerifier;
import reactor.core.publisher.Mono;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    @Mock
    private ReactiveTokenService tokenService;

    private TokenController controller;

    private static TokenQuery query() {
        TokenQuery query = new TokenQuery();
        query.setTenant("tenant-A");
        query.setName("alice");
        query.setSalt("0123456789abcdef0123456789abcdef");
        query.setPassword("hash");
        query.setNewPassword("new-hash");
        query.setToken("token");
        return query;
    }

    @BeforeEach
    void setUp() {
        controller = new TokenController(tokenService);
    }

    @Test
    void generateSaltReturnsDirectResource() {
        when(tokenService.generateSalt("alice", "tenant-A")).thenReturn(Mono.just("salt-value"));

        StepVerifier.create(controller.generateSalt(query()))
                .expectNext("salt-value")
                .verifyComplete();
    }

    @Test
    void generateSaltSignalsUnauthorizedWhenServiceReturnsNull() {
        when(tokenService.generateSalt("alice", "tenant-A")).thenReturn(Mono.error(new UnAuthorizedException("invalid")));

        StepVerifier.create(controller.generateSalt(query()))
                .expectError(UnAuthorizedException.class)
                .verify();
    }

    @Test
    void generateTokenReturnsDirectResourceAndSetsCookie() {
        when(tokenService.generateToken("alice", "hash", "tenant-A")).thenReturn(Mono.just("jwt-token"));
        ServerHttpResponse httpResponse = new MockServerHttpResponse();

        StepVerifier.create(controller.generateToken(query(), httpResponse))
                .expectNext("jwt-token")
                .verifyComplete();

        ResponseCookie cookie = httpResponse.getCookies().getFirst(RequestConstant.Header.TOKEN_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("jwt-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void generateTokenSignalsUnauthorizedWhenServiceReturnsNull() {
        when(tokenService.generateToken("alice", "hash", "tenant-A")).thenReturn(Mono.error(new UnAuthorizedException("invalid")));
        ServerHttpResponse httpResponse = new MockServerHttpResponse();

        StepVerifier.create(controller.generateToken(query(), httpResponse))
                .expectError(UnAuthorizedException.class)
                .verify();

        assertThat(httpResponse.getCookies().getFirst(RequestConstant.Header.TOKEN_COOKIE)).isNull();
    }

    @Test
    void changePasswordReturnsTrue() {
        when(tokenService.changePassword("alice", "hash", "new-hash", "tenant-A")).thenReturn(Mono.empty());

        StepVerifier.create(controller.changePassword(query()))
                .expectNext(Boolean.TRUE)
                .verifyComplete();
    }

    @Test
    void cancelTokenCompletesAndClearsCookie() {
        when(tokenService.tryCancelToken("alice", "tenant-A")).thenReturn(Mono.just(true));
        ServerHttpResponse httpResponse = new MockServerHttpResponse();

        StepVerifier.create(controller.cancelToken(query(), httpResponse))
                .verifyComplete();

        ResponseCookie cookie = httpResponse.getCookies().getFirst(RequestConstant.Header.TOKEN_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().isZero()).isTrue();
    }

    @Test
    void cancelTokenSignalsUnauthorizedWhenServiceRejects() {
        when(tokenService.tryCancelToken("alice", "tenant-A")).thenReturn(Mono.just(false));
        ServerHttpResponse httpResponse = new MockServerHttpResponse();

        StepVerifier.create(controller.cancelToken(query(), httpResponse))
                .expectError(UnAuthorizedException.class)
                .verify();
    }

    @Test
    void checkValidReturnsDirectResource() {
        TokenValid valid = new TokenValid(true, new Date(1_700_000_000_000L));
        when(tokenService.checkValid("alice", "token", "tenant-A")).thenReturn(Mono.just(valid));

        StepVerifier.create(controller.checkValid(query()))
                .expectNext(valid)
                .verifyComplete();
    }

    @Test
    void checkValidPreservesInvalidResultWithoutEnvelope() {
        TokenValid invalid = new TokenValid(false, null);
        when(tokenService.checkValid("alice", "token", "tenant-A")).thenReturn(Mono.just(invalid));

        StepVerifier.create(controller.checkValid(query()))
                .expectNext(invalid)
                .verifyComplete();
    }
}
