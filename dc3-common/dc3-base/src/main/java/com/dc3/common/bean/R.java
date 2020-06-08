/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Response
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean ok = false;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message = "fail";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    /**
     * 成功
     *
     * @return Response
     */
    @SuppressWarnings("unchecked")
    public static <T> R<T> ok() {
        return new R().success();
    }

    /**
     * 成功 自定义提示信息
     *
     * @return Response
     */
    @SuppressWarnings("unchecked")
    public static <T> R<T> ok(String message) {
        return new R().success(message);
    }

    /**
     * 成功 返回结果
     *
     * @param data 返回结果
     * @return Response
     */
    @SuppressWarnings("unchecked")
    public static <T> R<T> ok(T data) {
        return new R(data).success();
    }

    /**
     * 成功 返回结果 & 自定义提示信息
     *
     * @param data 返回结果
     * @return Response
     */
    @SuppressWarnings("unchecked")
    public static <T> R<T> ok(T data, String message) {
        return new R(data).success(message);
    }

    /**
     * 失败
     *
     * @return Response
     */
    @SuppressWarnings("unchecked")
    public static <T> R<T> fail() {
        return new R().failure();
    }

    /**
     * 失败 自定义提示信息
     *
     * @return Response
     */
    @SuppressWarnings("unchecked")
    public static <T> R<T> fail(String message) {
        return new R().failure(message);
    }

    /**
     * 失败 返回结果
     *
     * @param data 返回结果
     * @return Response
     */
    @SuppressWarnings("unchecked")
    public static <T> R<T> fail(T data) {
        return new R(data).failure();
    }

    /**
     * 失败 返回结果 & 自定义提示信息
     *
     * @param data 返回结果
     * @return Response
     */
    @SuppressWarnings("unchecked")
    public static <T> R<T> fail(T data, String message) {
        return new R(data).failure(message);
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
     * @return Response
     */
    private R success() {
        this.ok = true;
        this.message = "ok";
        return this;
    }

    /**
     * 成功 自定义提示信息
     *
     * @param message 成功提示信息
     * @return Response
     */
    private R success(String message) {
        this.ok = true;
        this.message = message;
        return this;
    }

    /**
     * 失败
     *
     * @return Response
     */
    private R failure() {
        this.ok = false;
        this.message = "fail";
        return this;
    }

    /**
     * 失败 自定义提示信息
     *
     * @param message 错误提示信息
     * @return Response
     */
    private R failure(String message) {
        this.ok = false;
        this.message = message;
        return this;
    }
}
