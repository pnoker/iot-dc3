/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        log.error("Global exception handler: {}", exception.getMessage(), exception);
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
