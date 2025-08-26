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
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;

/**
 * Exception 配置
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@RestControllerAdvice
public class ExceptionConfig {

    /**
     * Global Exception
     *
     * @param exception Exception
     * @param request   ServerHttpRequest
     * @return Mono R
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<R<String>> globalException(Exception exception, ServerHttpRequest request) {
        log.error("""
                Global exception
                Request: {}
                Exception: {}
                """, request.getURI().getRawPath(), exception.getMessage(), exception);
        return Mono.just(R.fail(exception.getMessage()));
    }

    /**
     * NotFound Exception
     *
     * @param exception NotFoundException
     * @param request   ServerHttpRequest
     * @return Mono R
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<R<String>> notFoundException(NotFoundException exception, ServerHttpRequest request) {
        log.warn("NotFound exception handler: {}", exception.getMessage(), exception);
        return Mono.just(R.fail(exception.getMessage()));
    }

    /**
     * UnAuthorized Exception
     *
     * @param exception UnAuthorizedException
     * @param request   ServerHttpRequest
     * @return Mono R
     */
    @ExceptionHandler(UnAuthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<R<String>> unAuthorizedException(UnAuthorizedException exception, ServerHttpRequest request) {
        log.warn("UnAuthorized exception handler: {}", exception.getMessage(), exception);
        return Mono.just(R.fail(exception.getMessage()));
    }

    /**
     * Validation Exception
     *
     * @param exception MethodArgumentNotValidException
     * @param request   ServerHttpRequest
     * @return Mono R
     */
    @ExceptionHandler({
            BindException.class,
            MethodArgumentNotValidException.class
    })
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Mono<R<String>> methodArgumentNotValidException(MethodArgumentNotValidException exception, ServerHttpRequest request) {
        HashMap<String, String> map = new HashMap<>(4);
        List<FieldError> errorList = exception.getBindingResult().getFieldErrors();
        errorList.forEach(error -> {
            log.warn("Method argument not valid exception handler: {}: {}", error.getField(), error.getDefaultMessage());
            map.put(error.getField(), error.getDefaultMessage());
        });
        return Mono.just(R.fail(JsonUtil.toJsonString(map)));
    }

}
