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
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.exception.BusinessException;
import io.github.pnoker.common.exception.PasswordChangeRequiredException;
import io.github.pnoker.common.exception.TenantNotScopedException;
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
 * Every {@link BusinessException} carries an {@link ErrorCode}; this handler reads that
 * code to align the response body code and the HTTP status, so the two never diverge.
 * Framework and validation exceptions are mapped to the closest {@link ErrorCode}.
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
     * Handle a password-change-required outcome. This is a routable login result, not a
     * hard failure, so it stays on HTTP 200 while carrying a distinct {@link ErrorCode}
     * ({@code R4031}/{@code R4032}) the client uses to open the password change flow.
     *
     * @param exception PasswordChangeRequiredException to handle
     * @param request   ServerHttpRequest that triggered the exception
     * @return Mono containing error response carrying a distinct response code
     */
    @ExceptionHandler(PasswordChangeRequiredException.class)
    @ResponseStatus(HttpStatus.OK)
    public Mono<R<String>> passwordChangeRequiredException(PasswordChangeRequiredException exception,
                                                           ServerHttpRequest request) {
        log.warn("Password change required, path={}, message={}", request.getURI().getRawPath(),
                exception.getMessage());
        return Mono.just(R.fail(exception.getErrorCode(), exception.getMessage()));
    }

    /**
     * Handle every {@link BusinessException}: read the carried {@link ErrorCode}, apply its
     * HTTP status to the response and return an envelope whose body code matches that status.
     *
     * @param exception BusinessException to handle
     * @param request   ServerHttpRequest that triggered the exception
     * @param response  ServerHttpResponse to which the carried HTTP status is applied
     * @return Mono containing error response
     */
    @ExceptionHandler(BusinessException.class)
    public Mono<R<String>> businessException(BusinessException exception, ServerHttpRequest request,
                                             ServerHttpResponse response) {
        ErrorCode errorCode = exception.getErrorCode();
        response.setStatusCode(HttpStatusCode.valueOf(errorCode.getHttpStatus()));

        String path = request.getURI().getRawPath();
        if (errorCode.getHttpStatus() >= 500) {
            log.error("Business exception {} on {}: {}", errorCode.getCode(), path, exception.getMessage(), exception);
        } else {
            log.warn("Business exception {} on {}: {}", errorCode.getCode(), path, exception.getMessage());
        }
        return Mono.just(R.fail(errorCode, exception.getMessage()));
    }

    /**
     * Handle Spring's framework-level {@link ResponseStatusException} — e.g. 404 from the
     * dispatcher when no handler matches a request. Preserves the original status on the
     * response and maps it to the closest {@link ErrorCode} so the body code stays aligned.
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
        return Mono.just(R.fail(mapStatusToErrorCode(status.value()),
                Objects.nonNull(reason) ? reason : status.toString()));
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
        return Mono.just(R.fail(ErrorCode.VALIDATION, JsonUtil.toJsonString(map)));
    }

    /**
     * Handle a {@link TenantNotScopedException}: a tenant-scoped query ran without a
     * tenant bound to the thread and the ignore flag unset — a programming error that
     * must surface as HTTP 500 rather than run unscoped.
     *
     * @param exception TenantNotScopedException to handle
     * @param request   ServerHttpRequest that triggered the exception
     * @return Mono containing error response carrying the failure code
     */
    @ExceptionHandler(TenantNotScopedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<R<String>> tenantNotScopedException(TenantNotScopedException exception, ServerHttpRequest request) {
        log.error("Tenant-scoped query executed without tenant context, path={}, message={}",
                request.getURI().getRawPath(), exception.getMessage(), exception);
        return Mono.just(R.fail(ErrorCode.FAILURE, "System error: tenant scope missing"));
    }

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
        return Mono.just(R.fail(ErrorCode.FAILURE, exception.getMessage()));
    }

    /**
     * Map a raw HTTP status to the closest {@link ErrorCode} so framework-raised statuses
     * still produce a meaningful, aligned body code.
     *
     * @param status HTTP status value
     * @return matching {@link ErrorCode}
     */
    private ErrorCode mapStatusToErrorCode(int status) {
        return switch (status) {
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 422 -> ErrorCode.VALIDATION;
            default -> ErrorCode.FAILURE;
        };
    }

}
