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

package io.github.pnoker.common.config;

import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ConflictException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionConfigTest {

    private ExceptionConfig handler;
    private ServerHttpRequest request;

    @BeforeEach
    void setUp() {
        handler = new ExceptionConfig();
        request = MockServerHttpRequest.get("/api/manager/devices").build();
    }

    @Test
    void globalExceptionProducesProblemDetails() {
        ServerHttpResponse httpResponse = new MockServerHttpResponse();
        StepVerifier.create(handler.globalException(new RuntimeException("boom"), request, httpResponse))
                .assertNext(problem -> {
                    assertThat(problem.status()).isEqualTo(500);
                    assertThat(problem.code()).isEqualTo(ErrorCode.FAILURE.getCode());
                    assertThat(problem.detail()).isEqualTo("Internal server error");
                })
                .verifyComplete();
    }

    @Test
    void responseStatusExceptionPreservesStatusAndReason() {
        ServerHttpResponse response = new MockServerHttpResponse();
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "downstream down");

        StepVerifier.create(handler.responseStatusException(ex, request, response))
                .assertNext(problem -> {
                    assertThat(problem.status()).isEqualTo(503);
                    assertThat(problem.detail()).isEqualTo("downstream down");
                })
                .verifyComplete();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void responseStatusExceptionWithoutReasonFallsBackToStatusToString() {
        ServerHttpResponse response = new MockServerHttpResponse();
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.responseStatusException(ex, request, response))
                .assertNext(problem -> assertThat(problem.detail()).contains("404"))
                .verifyComplete();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void requestExceptionAlignsBodyCodeAndStatusToValidation() {
        ServerHttpResponse response = new MockServerHttpResponse();
        StepVerifier.create(handler.businessException(new RequestException("invalid"), request, response))
                .assertNext(problem -> {
                    assertThat(problem.detail()).isEqualTo("invalid");
                    assertThat(problem.code()).isEqualTo(ErrorCode.VALIDATION.getCode());
                })
                .verifyComplete();
        assertThat(response.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void conflictExceptionProducesConflictProblemDetails() {
        ServerHttpResponse response = new MockServerHttpResponse();
        StepVerifier.create(handler.businessException(new ConflictException("stale version"), request, response))
                .assertNext(problem -> {
                    assertThat(problem.detail()).isEqualTo("stale version");
                    assertThat(problem.code()).isEqualTo(ErrorCode.CONFLICT.getCode());
                })
                .verifyComplete();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void notFoundExceptionAlignsBodyCodeAndStatusToNotFound() {
        ServerHttpResponse response = new MockServerHttpResponse();
        StepVerifier.create(handler.businessException(new NotFoundException("not here"), request, response))
                .assertNext(problem -> {
                    assertThat(problem.detail()).isEqualTo("not here");
                    assertThat(problem.code()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
                })
                .verifyComplete();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unAuthorizedExceptionAlignsBodyCodeAndStatusToUnauthorized() {
        ServerHttpResponse response = new MockServerHttpResponse();
        StepVerifier.create(handler.businessException(new UnAuthorizedException("nope"), request, response))
                .assertNext(problem -> {
                    assertThat(problem.detail()).isEqualTo("nope");
                    assertThat(problem.code()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
                })
                .verifyComplete();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void methodArgumentNotValidProducesJsonOfFieldErrors() {
        // Build a real BindingResult by using a mocked exception that returns a static
        // FieldError list. Going through the actual MethodArgumentNotValidException
        // constructor requires a MethodParameter and a real BindingResult, which is
        // overkill for this contract test.
        org.springframework.validation.BindingResult bindingResult =
                mock(org.springframework.validation.BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(
                new org.springframework.validation.FieldError("user", "name", "must not be blank"),
                new org.springframework.validation.FieldError("user", "age", "must be positive")));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ServerHttpResponse response = new MockServerHttpResponse();
        StepVerifier.create(handler.methodArgumentNotValidException(exception, request, response))
                .assertNext(problem -> {
                    assertThat(problem.errors().get("name")).containsExactly("must not be blank");
                    assertThat(problem.errors().get("age")).containsExactly("must be positive");
                })
                .verifyComplete();
    }
}
