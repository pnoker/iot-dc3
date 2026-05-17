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

import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Global Exception Handler Configuration
 * <p>
 * Global exception handler for reactive web applications using @RestControllerAdvice.
 * Provides centralized exception handling for common exceptions and validation errors,
 * returning standardized error responses.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@AutoConfiguration
@Slf4j
@RestControllerAdvice
public class ExceptionConfig {

    /**
     * Handle global exceptions
     *
     * @param exception Exception to handle
     * @param request   ServerHttpRequest that triggered the exception
     * @return Mono containing error response
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<R<String>> globalException(Exception exception, ServerHttpRequest request) {
        log.error("Global exception, path={}, message={}", request.getURI().getRawPath(), exception.getMessage(),
                exception);
        return Mono.just(R.fail(exception.getMessage()));
    }

    /**
     * Handle Spring's framework-level {@link ResponseStatusException} — e.g. 404 from the
     * dispatcher when no handler matches a request. These are not application failures,
     * so they must not surface as 500 ERROR entries. Preserves the original status on the
     * response and logs at a level appropriate to the status class.
     *
     * @param exception ResponseStatusException raised by the reactive dispatcher or an
     *                  HTTP client
     * @param request   ServerHttpRequest that triggered the exception
     * @param response  ServerHttpResponse to which the original status is applied
     * @return Mono containing error response
     */
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<R<String>> responseStatusException(ResponseStatusException exception, ServerHttpRequest request,
                                                   ServerHttpResponse response) {
        HttpStatusCode status = exception.getStatusCode();
        response.setStatusCode(status);

        String path = request.getURI().getRawPath();
        if (status.is5xxServerError()) {
            log.error("Response status exception {} on {}: {}", status.value(), path, exception.getMessage(),
                    exception);
        } else if (HttpStatus.NOT_FOUND.value() == status.value()) {
            log.debug("Not found: {}", path);
        } else {
            log.warn("Response status exception {} on {}: {}", status.value(), path, exception.getMessage());
        }

        String reason = exception.getReason();
        return Mono.just(R.fail(Objects.nonNull(reason) ? reason : status.toString()));
    }

    /**
     * Handle RequestException
     *
     * @param exception RequestException to handle
     * @param request   ServerHttpRequest that triggered the exception
     * @return Mono containing error response
     */
    @ExceptionHandler(RequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<R<String>> requestException(RequestException exception, ServerHttpRequest request) {
        log.warn("Request exception, path={}, message={}", request.getURI().getRawPath(), exception.getMessage());
        return Mono.just(R.fail(exception.getMessage()));
    }

    /**
     * Handle NotFoundException
     *
     * @param exception NotFoundException to handle
     * @param request   ServerHttpRequest that triggered the exception
     * @return Mono containing error response
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<R<String>> notFoundException(NotFoundException exception, ServerHttpRequest request) {
        log.warn("Not found exception, path={}, message={}", request.getURI().getRawPath(), exception.getMessage(),
                exception);
        return Mono.just(R.fail(exception.getMessage()));
    }

    /**
     * Handle UnAuthorizedException
     *
     * @param exception UnAuthorizedException to handle
     * @param request   ServerHttpRequest that triggered the exception
     * @return Mono containing error response
     */
    @ExceptionHandler(UnAuthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<R<String>> unAuthorizedException(UnAuthorizedException exception, ServerHttpRequest request) {
        log.warn("Unauthorized exception, path={}, message={}", request.getURI().getRawPath(), exception.getMessage(),
                exception);
        return Mono.just(R.fail(exception.getMessage()));
    }

    /**
     * Handle validation exceptions
     *
     * @param exception MethodArgumentNotValidException or BindException to handle
     * @param request   ServerHttpRequest that triggered the exception
     * @return Mono containing error response with field validation details
     */
    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public Mono<R<String>> methodArgumentNotValidException(MethodArgumentNotValidException exception,
                                                           ServerHttpRequest request) {
        HashMap<String, String> map = new HashMap<>(4);
        List<FieldError> errorList = exception.getBindingResult().getFieldErrors();
        errorList.forEach(error -> {
            log.warn("Method argument validation failed, path={}, field={}, message={}", request.getURI().getRawPath(),
                    error.getField(), error.getDefaultMessage());
            map.put(error.getField(), error.getDefaultMessage());
        });
        return Mono.just(R.fail(JsonUtil.toJsonString(map)));
    }

}
