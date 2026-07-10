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

package io.github.pnoker.common.resource.registrar.scan;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reports x-dc3-ai / description defects on an {@code @Operation} so a build or CI step can list
 * endpoints that need attention. Report-only by design: callers decide whether to fail the build.
 */
public class ApiAnnotationValidator {

    private static final int MIN_DESCRIPTION_LENGTH = 20;
    private static final Set<String> RISK_LEVELS = Set.of("HIGH", "MEDIUM", "LOW");
    private static final Set<String> BOOLEAN_KEYS = Set.of("destructive", "idempotent", "openWorld", "hidden");
    private static final String PROJECT_PACKAGE = "io.github.pnoker";
    private static final int MAX_BODY_DEPTH = 2;

    private static Map<String, String> aiProps(Operation operation) {
        Map<String, String> props = new java.util.LinkedHashMap<>();
        for (Extension extension : operation.extensions()) {
            if (!"x-dc3-ai".equalsIgnoreCase(extension.name())) {
                continue;
            }
            for (ExtensionProperty p : extension.properties()) {
                if (StringUtils.isNotBlank(p.name())) {
                    props.put(p.name(), StringUtils.defaultString(p.value()));
                }
            }
        }
        return props;
    }

    private static boolean hasAnnotation(java.lang.annotation.Annotation[] annotations, Class<?> type) {
        for (java.lang.annotation.Annotation a : annotations) {
            if (type.isInstance(a)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate an {@code @Operation} annotation: presence, description length, and the
     * {@code x-dc3-ai} extension keys (riskLevel and boolean flags).
     *
     * @param apiCode   the API code, for defect messages
     * @param operation the Operation annotation, may be null
     * @return the list of detected defects
     */
    public List<String> validate(String apiCode, Operation operation) {
        List<String> defects = new ArrayList<>();
        if (operation == null) {
            defects.add(apiCode + ": missing @Operation");
            return defects;
        }
        if (StringUtils.length(StringUtils.trimToEmpty(operation.description())) < MIN_DESCRIPTION_LENGTH) {
            defects.add(apiCode + ": description missing or shorter than " + MIN_DESCRIPTION_LENGTH + " chars");
        }
        Map<String, String> ai = aiProps(operation);
        if (ai.isEmpty()) {
            defects.add(apiCode + ": missing x-dc3-ai extension");
            return defects;
        }
        String risk = ai.get("riskLevel");
        if (risk == null || !RISK_LEVELS.contains(risk.toUpperCase())) {
            defects.add(apiCode + ": riskLevel missing or illegal (" + risk + ")");
        }
        for (String key : BOOLEAN_KEYS) {
            String value = ai.get(key);
            if (value != null && !"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                defects.add(apiCode + ": " + key + " is not true/false (" + value + ")");
            }
        }
        return defects;
    }

    /**
     * Validate one controller handler method: its {@code @Operation} (operation-level checks) plus the
     * descriptions of its request parameters that the OpenAPI aggregator merges into the MCP inputSchema —
     * {@code @RequestBody} object fields (via {@code @Schema}) and {@code @RequestParam}/{@code @PathVariable}
     * params (via {@code @Parameter}). Report-only: returns the defect list, never throws.
     */
    public List<String> validate(String apiCode, Method handlerMethod) {
        if (handlerMethod == null) {
            List<String> defects = new ArrayList<>();
            defects.add(apiCode + ": missing handler method");
            return defects;
        }
        List<String> defects = new ArrayList<>(validate(apiCode, handlerMethod.getAnnotation(Operation.class)));
        java.lang.reflect.Parameter[] parameters = handlerMethod.getParameters();
        java.lang.annotation.Annotation[][] paramAnnotations = handlerMethod.getParameterAnnotations();
        for (int i = 0; i < parameters.length; i++) {
            if (hasAnnotation(paramAnnotations[i], RequestBody.class)) {
                collectBodyDefects(apiCode, parameters[i].getType(), new HashSet<>(), 0, defects);
            } else if (hasAnnotation(paramAnnotations[i], RequestParam.class)
                    || hasAnnotation(paramAnnotations[i], PathVariable.class)) {
                Parameter doc = parameters[i].getAnnotation(Parameter.class);
                if (doc == null || StringUtils.isBlank(doc.description())) {
                    defects.add(apiCode + ": param " + parameters[i].getName() + " missing @Parameter(description)");
                }
            }
        }
        return defects;
    }

    /**
     * Walk a request-body type's instance fields, requiring @Schema(description) on each; recurse into nested project types.
     */
    private void collectBodyDefects(String apiCode, Class<?> type, Set<Class<?>> visited, int depth, List<String> defects) {
        if (type == null || depth > MAX_BODY_DEPTH || !visited.add(type)) {
            return;
        }
        for (Field field : type.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (java.lang.reflect.Modifier.isStatic(mods) || java.lang.reflect.Modifier.isTransient(mods)) {
                continue;
            }
            Schema schema = field.getAnnotation(Schema.class);
            if (schema == null || StringUtils.isBlank(schema.description())) {
                defects.add(apiCode + ": body field " + field.getName() + " missing @Schema(description)");
            }
            Class<?> fieldType = field.getType();
            if (fieldType.getName().startsWith(PROJECT_PACKAGE) && !fieldType.isEnum()) {
                collectBodyDefects(apiCode, fieldType, visited, depth + 1, defects);
            }
        }
    }
}
