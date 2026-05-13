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

import io.github.pnoker.common.exception.NotFoundException;
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
    void globalExceptionWrapsMessageInFailEnvelope() {
        StepVerifier.create(handler.globalException(new RuntimeException("boom"), request))
                .assertNext(response -> {
                    assertThat(response.isOk()).isFalse();
                    assertThat(response.getMessage()).isEqualTo("boom");
                })
                .verifyComplete();
    }

    @Test
    void responseStatusExceptionPreservesStatusAndReason() {
        ServerHttpResponse response = new MockServerHttpResponse();
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "downstream down");

        StepVerifier.create(handler.responseStatusException(ex, request, response))
                .assertNext(envelope -> {
                    assertThat(envelope.isOk()).isFalse();
                    assertThat(envelope.getMessage()).isEqualTo("downstream down");
                })
                .verifyComplete();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void responseStatusExceptionWithoutReasonFallsBackToStatusToString() {
        ServerHttpResponse response = new MockServerHttpResponse();
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.responseStatusException(ex, request, response))
                .assertNext(envelope -> assertThat(envelope.getMessage()).contains("404"))
                .verifyComplete();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void requestExceptionMapsToFailEnvelopeWithMessage() {
        StepVerifier.create(handler.requestException(new RequestException("invalid"), request))
                .assertNext(env -> assertThat(env.getMessage()).isEqualTo("invalid"))
                .verifyComplete();
    }

    @Test
    void notFoundExceptionMapsToFailEnvelope() {
        StepVerifier.create(handler.notFoundException(new NotFoundException("not here"), request))
                .assertNext(env -> assertThat(env.getMessage()).isEqualTo("not here"))
                .verifyComplete();
    }

    @Test
    void unAuthorizedExceptionMapsToFailEnvelope() {
        StepVerifier.create(handler.unAuthorizedException(new UnAuthorizedException("nope"), request))
                .assertNext(env -> assertThat(env.getMessage()).isEqualTo("nope"))
                .verifyComplete();
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

        StepVerifier.create(handler.methodArgumentNotValidException(exception, request))
                .assertNext(env -> {
                    assertThat(env.isOk()).isFalse();
                    assertThat(env.getMessage()).contains("\"name\":\"must not be blank\"");
                    assertThat(env.getMessage()).contains("\"age\":\"must be positive\"");
                })
                .verifyComplete();
    }
}
