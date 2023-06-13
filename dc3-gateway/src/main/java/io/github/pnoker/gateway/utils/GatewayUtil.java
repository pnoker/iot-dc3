/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.gateway.utils;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.exception.NotFoundException;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * 网关通用工具类
 *
 * @author pnoker
 * @since 2022.1.0
 */
public class GatewayUtil {

    private GatewayUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 获取远程客户端 IP
     *
     * @param request ServerHttpRequest
     * @return Remote Ip
     */
    public static String getRemoteIp(ServerHttpRequest request) {
        String ip = "";
        String[] headers = {"X-Original-Forwarded-For", "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        for (String header : headers) {
            ip = request.getHeaders().getFirst(header);
            if (!NetUtil.isUnknown(ip)) {
                return NetUtil.getMultistageReverseProxyIp(ip);
            }
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (!Objects.isNull(remoteAddress)) {
            ip = remoteAddress.getHostString();
        }
        return NetUtil.getMultistageReverseProxyIp(ip);
    }

    /**
     * 获取 Request Header
     *
     * @param request ServerHttpRequest
     * @param key     header key
     * @return request header value
     */
    public static String getRequestHeader(ServerHttpRequest request, String key) {
        String header = request.getHeaders().getFirst(key);
        if (!CharSequenceUtil.isNotEmpty(header)) {
            throw new NotFoundException("Invalid request header of " + key);
        }
        return header;
    }

    /**
     * 获取 Request Cookie
     *
     * @param request ServerHttpRequest
     * @param key     cookie key
     * @return request cookie value
     */
    public static String getRequestCookie(ServerHttpRequest request, String key) {
        HttpCookie cookie = request.getCookies().getFirst(key);
        if (ObjectUtil.isNull(cookie) || !CharSequenceUtil.isNotEmpty(cookie.getValue())) {
            throw new NotFoundException("Invalid request cookie of " + key);
        }
        return cookie.getValue();
    }
}
