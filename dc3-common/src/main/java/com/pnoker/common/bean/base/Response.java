/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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
package com.pnoker.common.bean.base;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 结果返回 实体类
 */
@Data
@NoArgsConstructor
public class Response {
    private boolean ok;
    private String message;
    private long time = System.currentTimeMillis();
    private Object result = null;

    public Response(Object result) {
        this.result = result;
    }

    /**
     * 成功
     *
     * @return
     */
    public Response ok() {
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
    public Response ok(String message) {
        this.ok = true;
        this.message = message;
        return this;
    }

    /**
     * 失败
     *
     * @return
     */
    public Response fail() {
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
    public Response fail(String message) {
        this.ok = false;
        this.message = message;
        return this;
    }
}
