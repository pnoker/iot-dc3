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

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves swagger-core serializes a custom {@code x-dc3-ai} extension into the OpenAPI model.
 *
 * <p>The jaxrs2 Reader ({@code io.swagger.v3.jaxrs2.Reader}) is not on this module's classpath
 * (swagger-jaxrs2-jakarta is not a dependency). This test uses the fallback approach:
 * it reads the {@code @Extension} annotation from a probe method via reflection, converts it to a
 * map using {@link AnnotationsUtils#getExtensions(Extension...)}, adds it to an OpenAPI Operation
 * model, and serializes to JSON — proving the full annotation → map → JSON pipeline.
 */
class SpringdocExtensionEmissionTest {

    /**
     * Probe method: carries the annotation under test.
     * Accessed only via reflection — never called directly.
     */
    @Operation(summary = "Probe", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "HIGH"),
            @ExtensionProperty(name = "destructive", value = "true")
    }))
    @SuppressWarnings("unused")
    private void annotatedProbe() {
    }

    @Test
    void extensionIsSerializedAsXDc3AiObject() throws Exception {
        // Step 1: read the @Operation annotation from the probe method via reflection.
        Method probe = SpringdocExtensionEmissionTest.class.getDeclaredMethod("annotatedProbe");
        Operation operationAnnotation = probe.getAnnotation(Operation.class);
        assertThat(operationAnnotation).as("probe method must carry @Operation").isNotNull();

        // Step 2: AnnotationsUtils.getExtensions converts @Extension[] → Map<String, Object>.
        // This is the same call springdoc makes internally when it processes @Operation.
        Map<String, Object> extensionsMap = AnnotationsUtils.getExtensions(operationAnnotation.extensions());
        assertThat(extensionsMap).as("annotation must produce a non-null extensions map").isNotNull();
        assertThat(extensionsMap).containsKey("x-dc3-ai");

        @SuppressWarnings("unchecked")
        Map<String, Object> dc3AiBlock = (Map<String, Object>) extensionsMap.get("x-dc3-ai");
        assertThat(dc3AiBlock).containsEntry("riskLevel", "HIGH");
        assertThat(dc3AiBlock).containsEntry("destructive", "true");

        // Step 3: add the extension to an OpenAPI model Operation and serialize to JSON.
        // This proves the map → JSON serialization path that the MCP aggregator will consume.
        io.swagger.v3.oas.models.Operation modelOperation = new io.swagger.v3.oas.models.Operation();
        extensionsMap.forEach(modelOperation::addExtension);
        String json = Json.pretty(modelOperation);

        assertThat(json).contains("x-dc3-ai");
        assertThat(json).contains("\"riskLevel\"");
        assertThat(json).contains("HIGH");
    }
}
