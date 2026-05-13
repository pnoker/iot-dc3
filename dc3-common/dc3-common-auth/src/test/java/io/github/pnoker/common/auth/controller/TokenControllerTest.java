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

package io.github.pnoker.common.auth.controller;

import io.github.pnoker.common.auth.biz.TokenService;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.auth.entity.query.TokenQuery;
import io.github.pnoker.common.entity.R;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Lightweight controller unit test that drives the reactive {@code Mono<R<...>>} via
 * StepVerifier without spinning up a Spring web context. This pins the controller's
 * envelope shaping (R.ok / R.fail / R.ok with composed message) and is much cheaper
 * to maintain than a full {@code @WebFluxTest} slice — slice variants land alongside
 * Testcontainers integration tests in a later stage.
 */
@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    @Mock
    private TokenService tokenService;

    private TokenController controller;

    private static TokenQuery query() {
        TokenQuery query = new TokenQuery();
        query.setTenant("tenant-A");
        query.setName("alice");
        query.setSalt("0123456789abcdef0123456789abcdef");
        query.setPassword("hash");
        query.setToken("token");
        return query;
    }

    @BeforeEach
    void setUp() {
        controller = new TokenController(tokenService);
    }

    @Test
    void generateSaltReturnsOkEnvelopeWithSaltMessage() {
        when(tokenService.generateSalt("alice", "tenant-A")).thenReturn("salt-value");

        StepVerifier.create(controller.generateSalt(query()))
                .assertNext(response -> {
                    assertThat(response.isOk()).isTrue();
                    assertThat(response.getData()).isEqualTo("salt-value");
                    assertThat(response.getMessage()).contains("5 minutes");
                })
                .verifyComplete();
    }

    @Test
    void generateSaltReturnsFailEnvelopeWhenServiceReturnsNull() {
        when(tokenService.generateSalt("alice", "tenant-A")).thenReturn(null);

        StepVerifier.create(controller.generateSalt(query()))
                .assertNext(response -> assertThat(response.isOk()).isFalse())
                .verifyComplete();
    }

    @Test
    void generateTokenSucceedsWithExpiryMessage() {
        when(tokenService.generateToken("alice",
                "0123456789abcdef0123456789abcdef", "hash", "tenant-A"))
                .thenReturn("jwt-token");

        StepVerifier.create(controller.generateToken(query()))
                .assertNext(response -> {
                    assertThat(response.isOk()).isTrue();
                    assertThat(response.getData()).isEqualTo("jwt-token");
                    assertThat(response.getMessage()).contains("12 hours");
                })
                .verifyComplete();
    }

    @Test
    void generateTokenReturnsFailWhenServiceReturnsNull() {
        when(tokenService.generateToken("alice",
                "0123456789abcdef0123456789abcdef", "hash", "tenant-A"))
                .thenReturn(null);

        StepVerifier.create(controller.generateToken(query()))
                .assertNext(response -> assertThat(response.isOk()).isFalse())
                .verifyComplete();
    }

    @Test
    void checkValidReturnsTrueWithRemainingExpiryMessage() {
        TokenValid valid = new TokenValid(true, new Date(1_700_000_000_000L));
        when(tokenService.checkValid("alice",
                "0123456789abcdef0123456789abcdef", "token", "tenant-A"))
                .thenReturn(valid);

        StepVerifier.create(controller.checkValid(query()))
                .assertNext(response -> {
                    assertThat(response.isOk()).isTrue();
                    assertThat(response.getData()).isTrue();
                    assertThat(response.getMessage()).startsWith("The token will expire in");
                })
                .verifyComplete();
    }

    @Test
    void checkValidReturnsFalseWithExpiredMessageWhenExpiryKnown() {
        TokenValid invalidWithExpiry = new TokenValid(false, new Date(1_700_000_000_000L));
        when(tokenService.checkValid("alice",
                "0123456789abcdef0123456789abcdef", "token", "tenant-A"))
                .thenReturn(invalidWithExpiry);

        StepVerifier.create(controller.checkValid(query()))
                .assertNext(response -> {
                    assertThat(response.isOk()).isTrue();
                    assertThat(response.getData()).isFalse();
                    assertThat(response.getMessage()).startsWith("The token has expired in");
                })
                .verifyComplete();
    }

    @Test
    void checkValidReturnsFalseWithGenericMessageWhenExpiryUnknown() {
        TokenValid invalidNoExpiry = new TokenValid(false, null);
        when(tokenService.checkValid("alice",
                "0123456789abcdef0123456789abcdef", "token", "tenant-A"))
                .thenReturn(invalidNoExpiry);

        StepVerifier.<R<Boolean>>create(controller.checkValid(query()))
                .assertNext(response -> {
                    assertThat(response.isOk()).isTrue();
                    assertThat(response.getData()).isFalse();
                    assertThat(response.getMessage()).isEqualTo("The token has expired");
                })
                .verifyComplete();
    }
}
