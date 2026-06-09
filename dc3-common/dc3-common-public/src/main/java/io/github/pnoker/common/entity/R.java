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

package io.github.pnoker.common.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.enums.ResponseEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Unified API response wrapper.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Unified API response wrapper. Every REST endpoint returns this envelope; the business payload is carried in 'data'.")
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Response status flag
     */
    @Schema(description = "Whether the request succeeded.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean ok = false;

    /**
     * Response status code
     */
    @Schema(description = "Business response code. 'R200' on success, 'R500' on a generic failure; see ResponseEnum for the full set.", example = "R200", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code = ResponseEnum.OK.getCode();

    /**
     * Response message
     */
    @Schema(description = "Human-readable response message.", example = "Success", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message = ResponseEnum.OK.getRemark();

    /**
     * Response data payload
     */
    @Schema(description = "Business data payload; null for operations that return no body or on failure.")
    @SuppressWarnings("all")
    private T data;

    /**
     * Private default constructor
     */
    private R() {
    }

    /**
     * Private constructor with data
     *
     * @param data Response data payload
     */
    private R(T data) {
        this.data = data;
    }

    /**
     * Create success response with default settings
     *
     * @param <T> Response data type
     * @return Response with success status
     */
    public static <T> R<T> ok() {
        return new R<T>().success();
    }

    /**
     * Create success response with custom message
     *
     * @param <T>     Response data type
     * @param message Custom success message
     * @return Response with success status and custom message
     */
    public static <T> R<T> ok(String message) {
        return new R<T>().success(message);
    }

    /**
     * Create success response with custom code and message
     *
     * @param <T>  Response data type
     * @param code {@link ResponseEnum} custom response code
     * @return Response with success status and custom code/message
     */
    public static <T> R<T> ok(ResponseEnum code) {
        return new R<T>().success(code.getCode(), code.getRemark());
    }

    /**
     * Success response with custom code and message.
     *
     * @param <T>     Object
     * @param code    {@link ResponseEnum}
     * @param message Success message
     * @return Response
     */
    public static <T> R<T> ok(ResponseEnum code, String message) {
        return new R<T>().success(code.getCode(), message);
    }

    /**
     * Success response with data.
     *
     * @param <T>  Object
     * @param data Response data
     * @return Response
     */
    public static <T> R<T> ok(T data) {
        return new R<T>(data).success();
    }

    /**
     * Success response with data and custom message.
     *
     * @param <T>     Object
     * @param data    Response data
     * @param message Success message
     * @return Response
     */
    public static <T> R<T> ok(T data, String message) {
        return new R<T>(data).success(message);
    }

    /**
     * Failure response with default settings.
     *
     * @param <T> Object
     * @return Response
     */
    public static <T> R<T> fail() {
        return new R<T>().failure();
    }

    /**
     * Failure response with custom message.
     *
     * @param <T>     Object
     * @param message Failure message
     * @return Response
     */
    public static <T> R<T> fail(String message) {
        return new R<T>().failure(message);
    }

    /**
     * Failure response with custom code.
     *
     * @param <T>  Object
     * @param code {@link ResponseEnum}
     * @return Response
     */
    public static <T> R<T> fail(ResponseEnum code) {
        return new R<T>().failure(code.getCode(), code.getRemark());
    }

    /**
     * Failure response with custom code and message.
     *
     * @param <T>     Object
     * @param code    {@link ResponseEnum}
     * @param message Failure message
     * @return Response
     */
    public static <T> R<T> fail(ResponseEnum code, String message) {
        return new R<T>().failure(code.getCode(), message);
    }

    /**
     * Failure response with data.
     *
     * @param <T>  Object
     * @param data Response data
     * @return Response
     */
    public static <T> R<T> fail(T data) {
        return new R<T>(data).failure();
    }

    /**
     * Failure response with data and custom message.
     *
     * @param <T>     Object
     * @param data    Response data
     * @param message Failure message
     * @return Response
     */
    public static <T> R<T> fail(T data, String message) {
        return new R<T>(data).failure(message);
    }

    /**
     * Internal success handler with default settings.
     *
     * @return Response
     */
    private R<T> success() {
        this.ok = true;
        this.code = ResponseEnum.OK.getCode();
        this.message = ResponseEnum.OK.getRemark();
        return this;
    }

    /**
     * Internal success handler with custom message.
     *
     * @param message Success message
     * @return Response
     */
    private R<T> success(String message) {
        this.ok = true;
        this.code = ResponseEnum.OK.getCode();
        this.message = message;
        return this;
    }

    /**
     * Internal success handler with custom code and message.
     *
     * @param code    Code
     * @param message Success message
     * @return Response
     */
    private R<T> success(String code, String message) {
        this.ok = true;
        this.code = code;
        this.message = message;
        return this;
    }

    /**
     * Internal failure handler with default settings.
     *
     * @return Response
     */
    private R<T> failure() {
        this.ok = false;
        this.code = ResponseEnum.FAILURE.getCode();
        this.message = ResponseEnum.FAILURE.getRemark();
        return this;
    }

    /**
     * Internal failure handler with custom message.
     *
     * @param message Error message
     * @return Response
     */
    private R<T> failure(String message) {
        this.ok = false;
        this.code = ResponseEnum.FAILURE.getCode();
        this.message = message;
        return this;
    }

    /**
     * Internal failure handler with custom code and message.
     *
     * @param code    Code
     * @param message Error message
     * @return Response
     */
    private R<T> failure(String code, String message) {
        this.ok = false;
        this.code = code;
        this.message = message;
        return this;
    }

}
