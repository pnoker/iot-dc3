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
package io.github.pnoker.common.auth.scan;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.common.utils.JsonUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Drift guard for the frozen {@code openapi-agentic.json} snapshot served to MCP/AI clients:
 * the snapshot's {@code METHOD:/path} set must equal the routes of the seven
 * dc3-common-agentic controllers, so a controller change without a snapshot resync (or vice
 * versa) fails the build instead of shipping 404s or stale contracts to external clients.
 * <p>
 * Controllers may hide individual endpoints from the snapshot via the {@code x-dc3-ai}
 * {@code hidden=true} extension property; those routes live in {@link #HIDDEN_ALLOWLIST},
 * which itself is checked against the annotations so an un-hidden route cannot silently
 * stay out of the snapshot.
 */
class AgenticOpenApiSnapshotGuardTest {

    private static final String CONTROLLER_PACKAGE = "io.github.pnoker.common.agentic.controller";
    private static final String SNAPSHOT_RESOURCE = "/openapi/openapi-agentic.json";
    private static final String AI_EXTENSION = "x-dc3-ai";

    /** Routes absent from the snapshot because their controller operation is hidden. */
    private static final Set<String> HIDDEN_ALLOWLIST = Set.of("POST:/provider/check", "POST:/model/config/check");

    private final ObjectMapper mapper = JsonUtil.getJsonMapper();

    @Test
    void snapshotPathsMatchControllerRoutes() {
        Map<String, Boolean> controllerRoutes = controllerRoutes();
        assertThat(controllerRoutes)
                .as(
                        "no @RestController found under %s; the test dependency on dc3-common-agentic is broken",
                        CONTROLLER_PACKAGE)
                .isNotEmpty();

        Set<String> hiddenRoutes = routesWhere(controllerRoutes, true);
        Set<String> publicRoutes = routesWhere(controllerRoutes, false);

        assertThat(hiddenRoutes)
                .as("allowlist entries that are no longer hidden on the controllers; remove them from HIDDEN_ALLOWLIST")
                .containsAll(HIDDEN_ALLOWLIST);
        assertThat(HIDDEN_ALLOWLIST)
                .as("controller routes newly hidden via x-dc3-ai hidden=true; add them to HIDDEN_ALLOWLIST")
                .containsAll(hiddenRoutes);

        Set<String> snapshotRoutes = snapshotRoutes();
        Set<String> missingFromSnapshot = minus(publicRoutes, snapshotRoutes);
        Set<String> staleInSnapshot = minus(snapshotRoutes, publicRoutes);
        assertThat(missingFromSnapshot)
                .as("controller routes missing from the frozen snapshot; resync openapi-agentic.json")
                .isEmpty();
        assertThat(staleInSnapshot)
                .as("snapshot routes that no longer exist on the controllers; resync openapi-agentic.json")
                .isEmpty();
    }

    private static Set<String> routesWhere(Map<String, Boolean> routes, boolean hidden) {
        Set<String> result = new TreeSet<>();
        routes.forEach((route, isHidden) -> {
            if (isHidden == hidden) {
                result.add(route);
            }
        });
        return result;
    }

    private static Set<String> minus(Set<String> left, Set<String> right) {
        Set<String> result = new TreeSet<>(left);
        result.removeAll(right);
        return result;
    }

    /** Reflect the agentic controllers into {@code METHOD:/path -> hidden?} route entries. */
    private Map<String, Boolean> controllerRoutes() {
        Map<String, Boolean> routes = new TreeMap<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.findCandidateComponents(CONTROLLER_PACKAGE).forEach(bean -> {
            Class<?> controller;
            try {
                controller = Class.forName(bean.getBeanClassName());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot load controller " + bean.getBeanClassName(), e);
            }
            for (String prefix : classPrefixes(controller)) {
                for (Method method : controller.getDeclaredMethods()) {
                    for (Map.Entry<String, List<String>> mapping :
                            methodMappings(method).entrySet()) {
                        for (String path : mapping.getValue()) {
                            routes.put(mapping.getKey() + ":" + join(prefix, path), isHidden(method));
                        }
                    }
                }
            }
        });
        return routes;
    }

    /** Class-level {@code @RequestMapping} path prefixes; a bare controller maps to the empty prefix. */
    private static List<String> classPrefixes(Class<?> controller) {
        RequestMapping request = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        if (request == null) {
            return List.of("");
        }
        return pathsOf(request.value(), request.path(), "");
    }

    /** Method-level mapping annotations to {@code verb -> paths} entries. */
    private Map<String, List<String>> methodMappings(Method method) {
        Map<String, List<String>> mappings = new TreeMap<>();
        GetMapping get = AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class);
        if (get != null) {
            mappings.put("GET", pathsOf(get.value(), get.path(), ""));
        }
        PostMapping post = AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class);
        if (post != null) {
            mappings.put("POST", pathsOf(post.value(), post.path(), ""));
        }
        PutMapping put = AnnotatedElementUtils.findMergedAnnotation(method, PutMapping.class);
        if (put != null) {
            mappings.put("PUT", pathsOf(put.value(), put.path(), ""));
        }
        DeleteMapping delete = AnnotatedElementUtils.findMergedAnnotation(method, DeleteMapping.class);
        if (delete != null) {
            mappings.put("DELETE", pathsOf(delete.value(), delete.path(), ""));
        }
        PatchMapping patch = AnnotatedElementUtils.findMergedAnnotation(method, PatchMapping.class);
        if (patch != null) {
            mappings.put("PATCH", pathsOf(patch.value(), patch.path(), ""));
        }
        RequestMapping request = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (request != null && request.method().length > 0) {
            for (RequestMethod verb : request.method()) {
                mappings.put(verb.name(), pathsOf(request.value(), request.path(), ""));
            }
        }
        return mappings;
    }

    private static List<String> pathsOf(String[] value, String[] path, String fallback) {
        String[] chosen = value.length > 0 ? value : path;
        if (chosen.length == 0) {
            return List.of(fallback);
        }
        return List.of(chosen);
    }

    private static String join(String prefix, String path) {
        if (prefix.isEmpty() || "/".equals(prefix)) {
            return path.isEmpty() ? "/" : path;
        }
        if (path.isEmpty()) {
            return prefix;
        }
        boolean prefixSlash = prefix.endsWith("/");
        boolean pathSlash = path.startsWith("/");
        if (prefixSlash && pathSlash) {
            return prefix + path.substring(1);
        }
        if (!prefixSlash && !pathSlash) {
            return prefix + "/" + path;
        }
        return prefix + path;
    }

    /** An operation is hidden when its {@code x-dc3-ai} extension carries {@code hidden=true}. */
    private static boolean isHidden(Method method) {
        Operation operation = AnnotatedElementUtils.findMergedAnnotation(method, Operation.class);
        if (operation == null) {
            return false;
        }
        for (Extension extension : operation.extensions()) {
            if (!AI_EXTENSION.equals(extension.name())) {
                continue;
            }
            for (ExtensionProperty property : extension.properties()) {
                if ("hidden".equals(property.name())) {
                    return Boolean.parseBoolean(property.value().toLowerCase(Locale.ROOT));
                }
            }
        }
        return false;
    }

    /** Parse the frozen snapshot's {@code paths} into {@code METHOD:/path} entries. */
    private Set<String> snapshotRoutes() {
        JsonNode paths;
        try (InputStream in = getClass().getResourceAsStream(SNAPSHOT_RESOURCE)) {
            assertThat(in)
                    .as("snapshot resource %s is missing from the classpath", SNAPSHOT_RESOURCE)
                    .isNotNull();
            paths = mapper.readTree(in).path("paths");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse " + SNAPSHOT_RESOURCE, e);
        }
        assertThat(paths.isObject())
                .as("snapshot %s has no paths object", SNAPSHOT_RESOURCE)
                .isTrue();
        Set<String> routes = new TreeSet<>();
        paths.properties().forEach(pathEntry -> {
            JsonNode pathItem = pathEntry.getValue();
            if (!pathItem.isObject()) {
                return;
            }
            pathItem.properties().forEach(operationEntry -> {
                String verb = operationEntry.getKey().toLowerCase(Locale.ROOT);
                if (operationEntry.getValue().isObject() && isHttpMethod(verb)) {
                    routes.add(verb.toUpperCase(Locale.ROOT) + ":" + pathEntry.getKey());
                }
            });
        });
        return routes;
    }

    private static boolean isHttpMethod(String key) {
        return Set.of("get", "post", "put", "delete", "patch", "head", "options", "trace")
                .contains(key);
    }
}
