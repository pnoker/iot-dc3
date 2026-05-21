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

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.facade.entity.bo.FacadeScannedApiBO;
import io.github.pnoker.common.resource.registrar.config.ResourceRegistrarProperties;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Walks the WebFlux handler mappings and turns every HTTP endpoint into a
 * {@link FacadeScannedApiBO}. One handler method with multiple HTTP methods or URL
 * patterns is fanned out to multiple entries, then de-duplicated by
 * {@code METHOD:path} so the auth-side sync receives a stable inventory.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@RequiredArgsConstructor
public class ApiEndpointScanner {

    private static final Set<RequestMethod> SUPPORTED_METHODS = Set.of(RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE);

    private static final List<String> DEFAULT_EXCLUDES = List.of("/actuator/**", "/error", "/error/**", "/favicon.ico");

    private final RequestMappingHandlerMapping handlerMapping;

    private final ResourceRegistrarProperties properties;

    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * Create a scanner bound to the WebFlux handler mapping built by the current
     * application context.
     *
     * @param handlerMapping WebFlux request mapping registry
     * @param properties     registrar scan configuration
     */

    private static String buildApiName(HandlerMethod handler) {
        String className = handler.getBeanType().getSimpleName();
        return className + "." + handler.getMethod().getName();
    }

    /**
     * Scan all registered WebFlux request mappings and return the endpoint inventory
     * visible to the permission resource registrar.
     *
     * @return deterministic list of scanned API endpoints
     */
    public List<FacadeScannedApiBO> scan() {
        Map<RequestMappingInfo, HandlerMethod> mappings = handlerMapping.getHandlerMethods();
        // Preserve discovery order for deterministic diffs.
        Map<String, FacadeScannedApiBO> deduped = new LinkedHashMap<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.entrySet()) {
            expand(entry.getKey(), entry.getValue(), deduped);
        }
        return new ArrayList<>(deduped.values());
    }

    /**
     * Expand one WebFlux mapping into zero or more permission endpoints. Unsupported
     * methods, ambiguous method-less mappings, excluded paths, and duplicate
     * {@code METHOD:path} entries are ignored.
     */
    private void expand(RequestMappingInfo info, HandlerMethod handler, Map<String, FacadeScannedApiBO> out) {
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        if (methods.isEmpty()) {
            // Controllers that don't declare a method expose GET by default at the HTTP
            // level, but for permission-registration purposes these are ambiguous - skip.
            return;
        }
        Set<PathPattern> patterns = info.getPatternsCondition().getPatterns();
        if (patterns.isEmpty()) {
            return;
        }
        for (PathPattern pattern : patterns) {
            String path = pattern.getPatternString();
            if (isExcluded(path)) {
                continue;
            }
            for (RequestMethod method : methods) {
                if (!SUPPORTED_METHODS.contains(method)) {
                    continue;
                }
                String key = method.name() + ":" + path;
                if (out.containsKey(key)) {
                    continue;
                }
                out.put(key,
                        FacadeScannedApiBO.builder()
                                .method(method.name())
                                .path(path)
                                .apiName(buildApiName(handler))
                                .title(handler.getMethod().getName())
                                .remark("")
                                .apiGroup(handler.getBeanType().getSimpleName())
                                .build());
            }
        }
    }

    /**
     * Apply built-in and user-configured Ant-style path excludes.
     */
    private boolean isExcluded(String path) {
        List<String> allExcludes = new ArrayList<>(DEFAULT_EXCLUDES);
        if (Objects.nonNull(properties.getExcludePaths())) {
            allExcludes.addAll(properties.getExcludePaths());
        }
        for (String pattern : allExcludes) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

}
