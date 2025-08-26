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

package io.github.pnoker.common.utils;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * 请求 相关工具类
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
public class RequestUtil {

    private RequestUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 获取远程客户端 IP
     *
     * @param request ServerHttpRequest
     * @return Remote Ip
     */
    public static String getRemoteIp(ServerHttpRequest request) {
        String ip = CharSequenceUtil.EMPTY;
        String[] headers = {"X-Original-Forwarded-For", "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        for (String header : headers) {
            ip = request.getHeaders().getFirst(header);
            if (!NetUtil.isUnknown(ip)) {
                return NetUtil.getMultistageReverseProxyIp(ip);
            }
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (Objects.nonNull(remoteAddress)) {
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
        return request.getHeaders().getFirst(key);
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
        return Objects.isNull(cookie) ? CharSequenceUtil.EMPTY : cookie.getValue();
    }
}
