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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Aggregates MCP tool input schemas from each center service's OpenAPI spec.
 *
 * <p>Only instantiated when {@code dc3.mcp.tool.aggregator.enabled=true}. It fetches
 * {@code /v3/api-docs} from each configured service, extracts the JSON request-body schema per
 * operation (resolving {@code $ref} shallowly), and keys it by
 * {@code serviceName:METHOD:path} so it lines up with {@code dc3_api.api_code}.
 *
 * <p>Best-effort: any fetch or parse failure for a service is logged and skipped — the catalog
 * still refreshes from {@code dc3_api}, only without that service's schema enrichment.
 *
 * @author pnoker
 * @version 2026.6.13
 * @since 2026.6.13
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dc3.mcp.tool.aggregator", name = "enabled", havingValue = "true")
public class McpOpenApiAggregator {

    private final McpAggregatorProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @return map of {@code serviceName:METHOD:path} -> JSON Schema string for the request body.
     */
    public Map<String, String> inputSchemasByApiCode() {
        Map<String, String> schemas = new HashMap<>();
        if (properties.getDocs() == null || properties.getDocs().isEmpty()) {
            return schemas;
        }
        for (Map.Entry<String, String> entry : properties.getDocs().entrySet()) {
            String serviceName = entry.getKey();
            String baseUrl = entry.getValue();
            try {
                String spec = WebClient.builder().baseUrl(baseUrl).build()
                        .get().uri("/v3/api-docs")
                        .retrieve().bodyToMono(String.class).block();
                if (StringUtils.isBlank(spec)) {
                    continue;
                }
                JsonNode root = objectMapper.readTree(spec);
                JsonNode paths = root.path("paths");
                if (paths.isMissingNode() || !paths.isObject()) {
                    continue;
                }
                paths.fields().forEachRemaining(pathEntry -> {
                    String path = pathEntry.getKey();
                    JsonNode pathItem = pathEntry.getValue();
                    if (!pathItem.isObject()) {
                        return;
                    }
                    pathItem.fields().forEachRemaining(opEntry -> {
                        String method = opEntry.getKey().toUpperCase();
                        JsonNode schema = opEntry.getValue()
                                .path("requestBody")
                                .path("content")
                                .path("application/json")
                                .path("schema");
                        if (schema.isMissingNode() || schema.isEmpty()) {
                            return;
                        }
                        try {
                            schemas.put(serviceName + ":" + method + ":" + path,
                                    objectMapper.writeValueAsString(resolveRefs(schema, root, 0)));
                        } catch (Exception ignore) {
                            // skip this operation on serialization failure
                        }
                    });
                });
            } catch (Exception ex) {
                log.warn("MCP aggregator: failed to fetch OpenAPI from {} ({}): {}",
                        serviceName, baseUrl, ex.getMessage());
            }
        }
        return schemas;
    }

    /**
     * Shallow recursive {@code $ref} resolver against {@code components/schemas}, depth-bounded to
     * avoid infinite loops on circular references.
     */
    private JsonNode resolveRefs(JsonNode node, JsonNode root, int depth) {
        if (node == null || node.isMissingNode() || depth > 6) {
            return node;
        }
        if (node.isObject()) {
            JsonNode ref = node.get("$ref");
            if (ref != null && ref.isTextual()) {
                JsonNode target = resolvePointer(ref.asText(), root);
                if (target != null) {
                    return resolveRefs(target, root, depth + 1);
                }
                return node;
            }
            ObjectNode copy = objectMapper.createObjectNode();
            node.fields().forEachRemaining(e -> copy.set(e.getKey(), resolveRefs(e.getValue(), root, depth + 1)));
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = objectMapper.createArrayNode();
            node.forEach(el -> copy.add(resolveRefs(el, root, depth + 1)));
            return copy;
        }
        return node;
    }

    private JsonNode resolvePointer(String pointer, JsonNode root) {
        if (pointer == null || !pointer.startsWith("#/")) {
            return null;
        }
        JsonNode current = root;
        for (String part : pointer.substring(2).split("/")) {
            current = current.path(part);
        }
        return current.isMissingNode() ? null : current;
    }
}
