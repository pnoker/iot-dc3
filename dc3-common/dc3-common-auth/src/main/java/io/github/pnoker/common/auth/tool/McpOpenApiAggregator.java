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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds MCP tool input schemas from static OpenAPI specs shipped on the auth classpath
 * under {@code openapi/openapi-*.json}.
 *
 * <p>The API contract (paths, parameters, request bodies, {@code @Schema} field docs) is a
 * compile-time fact that changes rarely, so it is snapshotted to a versioned file rather
 * than fetched at runtime. This removes the auth service's dependency on every center
 * service being reachable, and removes any need to expose {@code /v3/api-docs} in
 * production. Regenerate the snapshots with {@code make openapi} after a contract change.
 *
 * <p>Each file is named {@code openapi-<service>.json} where {@code <service>} is the bare
 * center name ({@code auth}/{@code manager}/{@code data}/{@code agentic}); it is expanded to
 * the full {@code dc3-center-<service>} so the resulting keys line up with
 * {@code dc3_api.api_code} ({@code dc3-center-manager:POST:/device/add}).
 *
 * <p>Best-effort: a missing directory, an unreadable file, or a parse failure for one
 * service is logged and skipped — the catalog still builds from {@code dc3_api}, only
 * without that service's schema enrichment.
 *
 * @author pnoker
 * @version 2026.6.18
 * @since 2026.6.13
 */
@Slf4j
@Component
public class McpOpenApiAggregator {

    private static final String SPECS_LOCATION = "classpath*:openapi/openapi-*.json";
    private static final String SERVICE_PREFIX = "dc3-center-";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static String text(JsonNode node) {
        return node.isTextual() && StringUtils.isNotBlank(node.asText()) ? node.asText() : null;
    }

    /**
     * Parse a declared "true"/"false" flag; null when absent or unparseable.
     */
    private static Boolean boolFlag(JsonNode ai, String key) {
        if (!ai.isObject()) {
            return null;
        }
        String value = text(ai.path(key));
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * @return map of {@code dc3-center-<service>:METHOD:/path} -> {@link ToolQuality} parsed from
     * each operation's summary/description, {@code x-dc3-ai} extension, and merged input schema.
     */
    public Map<String, ToolQuality> toolQualityByApiCode() {
        Map<String, ToolQuality> result = new HashMap<>();
        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(SPECS_LOCATION);
        } catch (Exception e) {
            log.warn("MCP aggregator: cannot resolve static OpenAPI specs at {}", SPECS_LOCATION, e);
            return result;
        }
        for (Resource resource : resources) {
            String serviceName = serviceNameOf(resource.getFilename());
            if (serviceName == null) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(in);
                JsonNode paths = root.path("paths");
                if (!paths.isObject()) {
                    continue;
                }
                paths.fields().forEachRemaining(pathEntry -> {
                    JsonNode pathItem = pathEntry.getValue();
                    if (!pathItem.isObject()) {
                        return;
                    }
                    pathItem.fields().forEachRemaining(opEntry -> {
                        String method = opEntry.getKey().toUpperCase();
                        JsonNode operation = opEntry.getValue();
                        if (!operation.isObject()) {
                            return;
                        }
                        String apiCode = serviceName + ":" + method + ":" + pathEntry.getKey();
                        result.put(apiCode, toQuality(operation, root));
                    });
                });
            } catch (Exception e) {
                log.warn("MCP aggregator: failed to read static OpenAPI spec {}", resource.getFilename(), e);
            }
        }
        return result;
    }

    private ToolQuality toQuality(JsonNode operation, JsonNode root) {
        JsonNode ai = operation.path("x-dc3-ai");
        String summary = text(operation.path("summary"));
        String operationDescription = text(operation.path("description"));
        String aiDescription = ai.isObject() ? text(ai.path("description")) : null;
        ObjectNode schema = buildOperationSchema(operation, root);
        String inputSchema = null;
        if (schema != null) {
            try {
                inputSchema = objectMapper.writeValueAsString(schema);
            } catch (Exception ignore) {
                // leave schema null on serialization failure
            }
        }
        return ToolQuality.builder()
                .summary(summary)
                .description(StringUtils.firstNonBlank(aiDescription, operationDescription))
                .riskLevel(ai.isObject() ? text(ai.path("riskLevel")) : null)
                .destructive(boolFlag(ai, "destructive"))
                .idempotent(boolFlag(ai, "idempotent"))
                .openWorld(boolFlag(ai, "openWorld"))
                .hidden(boolFlag(ai, "hidden"))
                .aiDescription(aiDescription)
                .inputSchema(inputSchema)
                .build();
    }

    /**
     * Expand {@code openapi-<service>.json} to the full {@code dc3-center-<service>} key prefix.
     * Returns {@code null} when the filename does not match the expected pattern.
     */
    private String serviceNameOf(String filename) {
        if (StringUtils.isBlank(filename) || !filename.startsWith("openapi-") || !filename.endsWith(".json")) {
            return null;
        }
        String bare = filename.substring("openapi-".length(), filename.length() - ".json".length());
        if (bare.isEmpty()) {
            return null;
        }
        return bare.startsWith(SERVICE_PREFIX) ? bare : SERVICE_PREFIX + bare;
    }

    /**
     * Build a unified MCP input schema for one operation by merging the JSON request body
     * (when present) with the operation's query and path {@code parameters}. Returns an
     * {@code object} schema with merged {@code properties}/{@code required}, or {@code null}
     * when the operation carries neither a body nor parameters.
     */
    ObjectNode buildOperationSchema(JsonNode operation, JsonNode root) {
        ObjectNode properties = objectMapper.createObjectNode();
        ArrayNode required = objectMapper.createArrayNode();

        // Request body: only merge object-shaped JSON bodies so their fields become properties.
        JsonNode bodySchema = operation.path("requestBody").path("content")
                .path("application/json").path("schema");
        if (!bodySchema.isMissingNode() && !bodySchema.isEmpty()) {
            JsonNode resolved = resolveRefs(bodySchema, root, 0);
            JsonNode bodyProps = resolved.path("properties");
            if (bodyProps.isObject()) {
                bodyProps.fields().forEachRemaining(f -> properties.set(f.getKey(), f.getValue()));
                resolved.path("required").forEach(required::add);
            }
        }

        // Query / path parameters: each becomes a property carrying its schema + description.
        JsonNode parameters = operation.path("parameters");
        if (parameters.isArray()) {
            for (JsonNode parameter : parameters) {
                String in = parameter.path("in").asText("");
                String name = parameter.path("name").asText("");
                if (name.isEmpty() || (!"query".equals(in) && !"path".equals(in))) {
                    continue;
                }
                JsonNode paramSchema = resolveRefs(parameter.path("schema"), root, 0);
                ObjectNode property = paramSchema.isObject()
                        ? ((ObjectNode) paramSchema).deepCopy() : objectMapper.createObjectNode();
                String description = parameter.path("description").asText("");
                if (!description.isEmpty() && !property.has("description")) {
                    property.put("description", description);
                }
                properties.set(name, property);
                // Path params are always required; query params follow their declared flag.
                if ("path".equals(in) || parameter.path("required").asBoolean(false)) {
                    required.add(name);
                }
            }
        }

        if (properties.isEmpty()) {
            return null;
        }
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", properties);
        if (!required.isEmpty()) {
            schema.set("required", required);
        }
        return schema;
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
