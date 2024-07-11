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

package io.github.pnoker.common.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.enums.ResponseEnum;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Response
 *
 * @author pnoker
 * @since 2022.1.0
 */

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应状态
     */
    private boolean ok = false;

    /**
     * 响应编码
     */
    private String code = ResponseEnum.OK.getCode();

    /**
     * 响应信息
     */
    private String message = ResponseEnum.OK.getText();

    /**
     * 响应数据
     */
    @SuppressWarnings("all")
    private T data;

    /**
     * 构造函数
     */
    private R() {
    }

    /**
     * 构造函数
     *
     * @param data 数据
     */
    private R(T data) {
        this.data = data;
    }

    /**
     * 成功
     *
     * @param <T> Object
     * @return Response
     */
    public static <T> R<T> ok() {
        return new R<T>().success();
    }

    /**
     * 成功 自定义提示信息
     *
     * @param <T>     Object
     * @param message 成功信息
     * @return Response
     */
    public static <T> R<T> ok(String message) {
        return new R<T>().success(message);
    }

    /**
     * 成功 自定义 Code 和 提示信息ff
     *
     * @param <T>  Object
     * @param code {@link ResponseEnum}
     * @return Response
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
