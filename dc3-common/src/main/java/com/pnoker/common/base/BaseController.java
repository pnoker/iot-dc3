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
package com.pnoker.common.base;

import com.pnoker.common.bean.base.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: The class Base controller.
 */
@Slf4j
public class BaseController {
    /**
     * 获取 HttpServletRequest
     *
     * @return
     */
    protected HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    /**
     * 根据文件相对目录获取服务器上目录的真实位置
     *
     * @param resource
     * @return
     */
    public String getResourcePath(String resource) {
        String path = BaseController.class.getResource("/").getPath();
        try {
            path = URLDecoder.decode(path, "UTF-8") + resource;
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        }
        return path;
    }

    /**
     * 成功
     *
     * @return
     */
    public Response ok() {
        return new Response().ok();
    }

    /**
     * 成功 自定义提示信息
     *
     * @return
     */
    public Response ok(String message) {
        return new Response().ok(message);
    }

    /**
     * 成功 返回结果
     *
     * @param object 返回结果
     * @return
     */
    public Response ok(Object object) {
        return new Response(object).ok();
    }

    /**
     * 成功 返回结果 & 自定义提示信息
     *
     * @param object 返回结果
     * @return
     */
    public Response ok(Object object, String message) {
        return new Response(object).ok(message);
    }

    /**
     * 失败
     *
     * @return
     */
    public Response fail() {
        return new Response().fail();
    }

    /**
     * 失败 自定义提示信息
     *
     * @return
     */
    public Response fail(String message) {
        return new Response().fail(message);
    }

    /**
     * 失败 返回结果
     *
     * @param object 返回结果
     * @return
     */
    public Response fail(Object object) {
        return new Response(object).fail();
    }

    /**
     * 失败 返回结果 & 自定义提示信息
     *
     * @param object 返回结果
     * @return
     */
    public Response fail(Object object, String message) {
        return new Response(object).fail(message);
    }
}
