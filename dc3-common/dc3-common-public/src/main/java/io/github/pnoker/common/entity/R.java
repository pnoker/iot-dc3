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
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Unified Response Wrapper Class
 * <p>
 * Generic response wrapper class for API responses in IoT DC3 platform.
 * Provides standardized response format with status, code, message, and data fields.
 * Supports both success and failure response scenarios.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Response status flag
     */
    private boolean ok = false;

    /**
     * Response status code
     */
    private String code = ResponseEnum.OK.getCode();

    /**
     * Response message
     */
    private String message = ResponseEnum.OK.getText();

    /**
     * Response data payload
     */
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
     * @param <T>     Response data type
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
     * @param <T>     Response data type
     * @param code    {@link ResponseEnum} custom response code
     * @return Response with success status and custom code/message
     */
    public static <T> R<T> ok(ResponseEnum code) {
        return new R<T>().success(code.getCode(), code.getText());
    }

    /**
     * 成功 自定义 Code 和 提示信息
     *
     * @param <T>     Object
     * @param code    {@link ResponseEnum}
     * @param message 成功信息
     * @return Response
     */
    public static <T> R<T> ok(ResponseEnum code, String message) {
        return new R<T>().success(code.getCode(), message);
    }

    /**
     * 成功 返回结果
     *
     * @param <T>  Object
     * @param data 返回结果
     * @return Response
     */
    public static <T> R<T> ok(T data) {
        return new R<T>(data).success();
    }

    /**
     * 成功 返回结果 和 自定义提示信息
     *
     * @param <T>     Object
     * @param data    返回结果
     * @param message 成功信息
     * @return Response
     */
    public static <T> R<T> ok(T data, String message) {
        return new R<T>(data).success(message);
    }

    /**
     * 失败
     *
     * @param <T> Object
     * @return Response
     */
    public static <T> R<T> fail() {
        return new R<T>().failure();
    }

    /**
     * 失败 自定义提示信息
     *
     * @param <T>     Object
     * @param message 失败信息
     * @return Response
     */
    public static <T> R<T> fail(String message) {
        return new R<T>().failure(message);
    }

    /**
     * 失败 自定义 Code 和 提示信息
     *
     * @param <T>  Object
     * @param code {@link ResponseEnum}
     * @return Response
     */
    public static <T> R<T> fail(ResponseEnum code) {
        return new R<T>().failure(code.getCode(), code.getText());
    }

    /**
     * 失败 自定义 Code 和 提示信息
     *
     * @param <T>     Object
     * @param code    {@link ResponseEnum}
     * @param message 失败信息
     * @return Response
     */
    public static <T> R<T> fail(ResponseEnum code, String message) {
        return new R<T>().failure(code.getCode(), message);
    }

    /**
     * 失败 返回结果
     *
     * @param <T>  Object
     * @param data 返回结果
     * @return Response
     */
    public static <T> R<T> fail(T data) {
        return new R<T>(data).failure();
    }

    /**
     * 失败 返回结果 和 自定义提示信息
     *
     * @param <T>     Object
     * @param data    返回结果
     * @param message 失败信息
     * @return Response
     */
    public static <T> R<T> fail(T data, String message) {
        return new R<T>(data).failure(message);
    }

    /**
     * 成功
     *
     * @return Response
     */
    private R<T> success() {
        this.ok = true;
        this.code = ResponseEnum.OK.getCode();
        this.message = ResponseEnum.OK.getText();
        return this;
    }

    /**
     * 成功 自定义提示信息
     *
     * @param message 成功提示信息
     * @return Response
     */
    private R<T> success(String message) {
        this.ok = true;
        this.code = ResponseEnum.OK.getCode();
        this.message = message;
        return this;
    }

    /**
     * 成功 自定义提示信息
     *
     * @param code    Code
     * @param message 成功提示信息
     * @return Response
     */
    private R<T> success(String code, String message) {
        this.ok = true;
        this.code = code;
        this.message = message;
        return this;
    }

    /**
     * 失败
     *
     * @return Response
     */
    private R<T> failure() {
        this.ok = false;
        this.code = ResponseEnum.FAILURE.getCode();
        this.message = ResponseEnum.FAILURE.getText();
        return this;
    }

    /**
     * 失败 自定义提示信息
     *
     * @param message 错误提示信息
     * @return Response
     */
    private R<T> failure(String message) {
        this.ok = false;
        this.code = ResponseEnum.FAILURE.getCode();
        this.message = message;
        return this;
    }

    /**
     * 失败 自定义提示信息
     *
     * @param code    Code
     * @param message 错误提示信息
     * @return Response
     */
    private R<T> failure(String code, String message) {
        this.ok = false;
        this.code = code;
        this.message = message;
        return this;
    }

}
