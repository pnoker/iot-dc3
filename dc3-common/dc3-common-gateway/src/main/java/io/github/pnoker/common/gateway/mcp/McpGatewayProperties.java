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

package io.github.pnoker.common.gateway.mcp;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gateway MCP endpoint and backend invocation configuration.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.mcp.gateway")
public class McpGatewayProperties {

    /**
     * Public MCP resource URI advertised by protected resource metadata.
     */
    private String resource = "http://localhost:8000/mcp";

    /**
     * Public authorization server URI advertised by protected resource metadata.
     */
    private String authorizationServer = "http://localhost:8000";

    /**
     * Backend base URLs keyed by DC3 service name.
     */
    @NotEmpty(message = "MCP backend base urls can't be empty")
    private Map<String, String> backendBaseUrls = new LinkedHashMap<>();

    public String backendBaseUrl(String serviceName) {
        String baseUrl = backendBaseUrls.get(serviceName);
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("Unknown backend service: " + serviceName);
        }
        return StringUtils.removeEnd(baseUrl, "/");
    }

}
