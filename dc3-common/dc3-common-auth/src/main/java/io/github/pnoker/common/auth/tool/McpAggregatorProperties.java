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

package io.github.pnoker.common.auth.tool;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for the optional OpenAPI-based MCP tool catalog aggregator.
 *
 * <p>Off by default. When {@code dc3.mcp.tool.aggregator.enabled=true}, the
 * {@link McpOpenApiAggregator} fetches each center service's {@code /v3/api-docs} and derives a
 * JSON request schema per tool; otherwise the catalog keeps building purely from
 * {@code dc3_api} with zero cross-service calls.
 *
 * @author pnoker
 * @version 2026.6.13
 * @since 2026.6.13
 */
@Data
@Component
@ConfigurationProperties(prefix = "dc3.mcp.tool.aggregator")
public class McpAggregatorProperties {

    /**
     * Master switch. Off by default so the default refresh path is unchanged.
     */
    private boolean enabled = false;

    /**
     * {@code dc3_api.service_name} -> base URL that serves {@code /v3/api-docs}, e.g.
     * {@code dc3-center-manager -> http://dc3-center-manager:8400}.
     */
    private Map<String, String> docs = new LinkedHashMap<>();
}
