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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers {@link McpOpenApiAggregator#buildOperationSchema} — the merge of JSON request body
 * fields with query/path parameters into one MCP input schema.
 */
class McpOpenApiAggregatorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private final McpOpenApiAggregator aggregator = new McpOpenApiAggregator();

    @Test
    void mergesRequestBodyFieldsWithQueryAndPathParameters() throws Exception {
        JsonNode operation = mapper.readTree("""
                {
                  "parameters": [
                    {"name": "tenantId", "in": "path", "schema": {"type": "integer"}},
                    {"name": "keyword", "in": "query", "required": true,
                     "description": "search keyword", "schema": {"type": "string"}},
                    {"name": "page", "in": "query", "schema": {"type": "integer"}}
                  ],
                  "requestBody": {"content": {"application/json": {"schema": {
                    "type": "object",
                    "properties": {"deviceName": {"type": "string"}},
                    "required": ["deviceName"]
                  }}}}
                }
                """);

        ObjectNode schema = aggregator.buildOperationSchema(operation, mapper.createObjectNode());

        assertThat(schema).isNotNull();
        assertThat(schema.get("type").asText()).isEqualTo("object");
        JsonNode props = schema.get("properties");
        assertThat(props.has("deviceName")).isTrue();
        assertThat(props.has("tenantId")).isTrue();
        assertThat(props.has("keyword")).isTrue();
        assertThat(props.get("keyword").get("description").asText()).isEqualTo("search keyword");
        assertThat(props.has("page")).isTrue();
        // deviceName (body required), tenantId (path always), keyword (query required) — page is optional.
        assertThat(schema.get("required")).extracting(JsonNode::asText)
                .containsExactlyInAnyOrder("deviceName", "tenantId", "keyword");
    }

    @Test
    void queryOnlyOperationStillProducesSchema() throws Exception {
        JsonNode operation = mapper.readTree("""
                {
                  "parameters": [
                    {"name": "id", "in": "query", "required": true, "schema": {"type": "integer"}}
                  ]
                }
                """);

        ObjectNode schema = aggregator.buildOperationSchema(operation, mapper.createObjectNode());

        assertThat(schema).isNotNull();
        assertThat(schema.get("properties").has("id")).isTrue();
        assertThat(schema.get("required")).extracting(JsonNode::asText).containsExactly("id");
    }

    @Test
    void loadsStaticSpecFromClasspathWithFullServiceNameKeys() throws Exception {
        Map<String, String> schemas = aggregator.inputSchemasByApiCode();

        // Keys expand the file's bare service name to dc3-center-<service> to match api_code.
        String addKey = "dc3-center-fixturesvc:POST:/device/add";
        String listKey = "dc3-center-fixturesvc:POST:/device/list_by_ids";
        assertThat(schemas).containsKeys(addKey, listKey);

        JsonNode add = mapper.readTree(schemas.get(addKey));
        assertThat(add.get("properties").has("deviceName")).isTrue();
        assertThat(add.get("properties").has("driverId")).isTrue();
        assertThat(add.get("required")).extracting(JsonNode::asText).containsExactly("deviceName");

        // A body-less POST that only declares a query param still yields a schema.
        JsonNode list = mapper.readTree(schemas.get(listKey));
        assertThat(list.get("properties").has("page")).isTrue();
    }

    @Test
    void operationWithoutBodyOrParametersReturnsNull() throws Exception {
        JsonNode operation = mapper.readTree("{}");

        assertThat(aggregator.buildOperationSchema(operation, mapper.createObjectNode())).isNull();
    }

    @Test
    void headerParametersAreIgnored() throws Exception {
        JsonNode operation = mapper.readTree("""
                {
                  "parameters": [
                    {"name": "X-Trace", "in": "header", "schema": {"type": "string"}}
                  ]
                }
                """);

        // Only query/path params count; a header-only operation yields no schema.
        assertThat(aggregator.buildOperationSchema(operation, mapper.createObjectNode())).isNull();
    }
}
