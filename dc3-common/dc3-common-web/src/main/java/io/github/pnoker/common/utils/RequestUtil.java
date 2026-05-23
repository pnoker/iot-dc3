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

import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Utility class for handling HTTP request operations. Provides methods to extract
 * information from ServerHttpRequest objects such as IP addresses, headers and cookies.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class RequestUtil {

    private static final String[] PROXY_IP_HEADERS = {
            "X-Original-Forwarded-For", "X-Forwarded-For", "X-Real-IP",
            "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
    };
    private static final String UNKNOWN_IP = "unknown";
    private static final String COMMA = ",";

    private RequestUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Extracts the first valid IP address from a comma-separated list of IPs. Used for
     * handling X-Forwarded-For and similar headers in reverse proxy scenarios.
     *
     * @param ip Comma-separated list of IP addresses
     * @return The first valid IP address found, or null if none is valid
     */
    public static String getMultistageReverseProxyIp(String ip) {
        if (Objects.isNull(ip)) {
            return null;
        }
        String[] ips = ip.split(COMMA);
        for (String s : ips) {
            s = s.trim();
            if (!s.isEmpty() && !UNKNOWN_IP.equalsIgnoreCase(s)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Gets the remote client IP address from the request. Checks various headers commonly
     * used in proxy scenarios before falling back to the remote address. Headers checked:
     * X-Original-Forwarded-For, X-Forwarded-For, X-Real-IP, Proxy-Client-IP,
     * WL-Proxy-Client-IP, HTTP_CLIENT_IP, HTTP_X_FORWARDED_FOR
     *
     * @param request ServerHttpRequest object containing the request information
     * @return The client's IP address as a string
     */
    public static String getRemoteIp(ServerHttpRequest request) {
        String ip = StringUtils.EMPTY;
        for (String header : PROXY_IP_HEADERS) {
            ip = request.getHeaders().getFirst(header);
            if (!(Objects.isNull(ip) || ip.isEmpty() || UNKNOWN_IP.equalsIgnoreCase(ip))) {
                return getMultistageReverseProxyIp(ip);
            }
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (Objects.nonNull(remoteAddress)) {
            ip = remoteAddress.getHostString();
        }
        return getMultistageReverseProxyIp(ip);
    }

    /**
     * Retrieves a specific header value from the request. Returns the first value if
     * multiple values exist for the same header.
     *
     * @param request ServerHttpRequest object containing the request information
     * @param key     The name of the header to retrieve
     * @return The value of the specified header, or null if not present
     */
    public static String getRequestHeader(ServerHttpRequest request, String key) {
        return request.getHeaders().getFirst(key);
    }

    /**
     * Retrieves a specific cookie value from the request. Returns empty string if the
     * cookie is not present.
     *
     * @param request ServerHttpRequest object containing the request information
     * @param key     The name of the cookie to retrieve
     * @return The value of the specified cookie, or empty string if not present
     */
    public static String getRequestCookie(ServerHttpRequest request, String key) {
        HttpCookie cookie = request.getCookies().getFirst(key);
        return Objects.isNull(cookie) ? StringUtils.EMPTY : cookie.getValue();
    }

}
