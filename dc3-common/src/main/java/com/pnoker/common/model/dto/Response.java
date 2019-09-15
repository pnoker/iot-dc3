/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.pnoker.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author    : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 返回信息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {
    private boolean ok;
    private String message;
    private long time = System.currentTimeMillis();
    private T data;

    /**
     * 成功
     *
     * @return
     */
    public static <T> Response<T> ok() {
        return new Response().success();
    }

    /**
     * 成功 自定义提示信息
     *
     * @return
     */
    public static <T> Response<T> ok(String message) {
        return new Response().success(message);
    }

    /**
     * 成功 返回结果
     *
     * @param data 返回结果
     * @return
     */
    public static <T> Response<T> ok(T data) {
        return new Response(data).success();
    }

    /**
     * 成功 返回结果 & 自定义提示信息
     *
     * @param data 返回结果
     * @return
     */
    public static <T> Response<T> ok(T data, String message) {
        return new Response(data).success(message);
    }

    /**
     * 失败
     *
     * @return
     */
    public static <T> Response<T> fail() {
        return new Response().failure();
    }

    /**
     * 失败 自定义提示信息
     *
     * @return
     */
    public static <T> Response<T> fail(String message) {
        return new Response().failure(message);
    }

    /**
     * 失败 返回结果
     *
     * @param data 返回结果
     * @return
     */
    public static <T> Response<T> fail(T data) {
        return new Response(data).failure();
    }

    /**
     * 失败 返回结果 & 自定义提示信息
     *
     * @param data 返回结果
     * @return
     */
    public static <T> Response<T> fail(T data, String message) {
        return new Response(data).failure(message);
    }

    /**
     * 构造函数
     *
     * @param data
     */
    public Response(T data) {
        this.data = data;
    }

    /**
     * 成功
     *
     * @return
     */
    public Response success() {
        this.ok = true;
        this.message = "Ok,Request Succeeded!";
        return this;
    }

    /**
     * 成功 自定义提示信息
     *
     * @param message
     * @return
     */
    public Response success(String message) {
        this.ok = true;
        this.message = message;
        return this;
    }

    /**
     * 失败
     *
     * @return
     */
    public Response failure() {
        this.ok = false;
        this.message = "Sorry,Request Failed!";
        return this;
    }

    /**
     * 失败 自定义提示信息
     *
     * @param message
     * @return
     */
    public Response failure(String message) {
        this.ok = false;
        this.message = message;
        return this;
    }
}
