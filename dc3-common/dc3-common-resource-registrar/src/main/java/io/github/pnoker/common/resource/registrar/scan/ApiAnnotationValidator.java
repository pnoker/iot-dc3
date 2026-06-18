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
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
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
}
