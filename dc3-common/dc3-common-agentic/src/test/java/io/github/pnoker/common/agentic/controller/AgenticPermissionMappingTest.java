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
package io.github.pnoker.common.agentic.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Ratchet for the agentic permission model. Two guards:
 *
 * <ul>
 *   <li>every endpoint's {@code @perm.can(domain, scope)} must match the expected
 *       verb map (platform five-verb convention; high-risk state changes need
 *       {@code update}, creation needs {@code add}) — a changed or added endpoint
 *       fails here until the map is consciously updated;</li>
 *   <li>every distinct permission code must exist in the dc3_resource seed so normal
 *       users can actually be granted it (only the wildcard admin worked before).</li>
 * </ul>
 */
class AgenticPermissionMappingTest {

    private static final Pattern PERMISSION_CAN_PATTERN =
            Pattern.compile("@perm\\.can\\(\\s*['\"]([^'\"]+)['\"]\\s*,\\s*['\"]([^'\"]+)['\"]\\s*\\)");

    private static final String SERVICE_NAME = "dc3-center-agentic";

    /** Expected verb per endpoint, keyed by Controller#method. */
    private static final Map<String, String> EXPECTED_VERBS = buildExpectedVerbs();

    private static Map<String, String> buildExpectedVerbs() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("ActionController#pending", "action:list");
        expected.put("ActionController#confirm", "action:update");
        expected.put("ActionController#reject", "action:update");
        expected.put("AttachmentController#upload", "attachment:add");
        expected.put("AttachmentController#list", "attachment:list");
        expected.put("AttachmentController#delete", "attachment:delete");
        expected.put("ChatController#chatCompletion", "chat:add");
        expected.put("MessageController#list", "message:list");
        expected.put("ModelController#list", "model:list");
        expected.put("ModelController#listConfigs", "model:list");
        expected.put("ModelController#add", "model:add");
        expected.put("ModelController#update", "model:update");
        expected.put("ModelController#delete", "model:delete");
        expected.put("ModelController#check", "model:get");
        expected.put("ProviderController#list", "provider:list");
        expected.put("ProviderController#add", "provider:add");
        expected.put("ProviderController#update", "provider:update");
        expected.put("ProviderController#delete", "provider:delete");
        expected.put("ProviderController#check", "provider:get");
        expected.put("SessionController#list", "session:list");
        expected.put("SessionController#get", "session:get");
        expected.put("SessionController#update", "session:update");
        expected.put("SessionController#delete", "session:delete");
        return expected;
    }

    private static final List<Class<?>> CONTROLLERS = List.of(
            ActionController.class,
            AttachmentController.class,
            ChatController.class,
            MessageController.class,
            ModelController.class,
            ProviderController.class,
            SessionController.class);

    @Test
    void everyEndpointCarriesTheExpectedPermissionVerb() {
        Map<String, String> actual = new TreeMap<>();
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!hasMapping(method)) {
                    continue;
                }
                actual.put(controller.getSimpleName() + "#" + method.getName(), permissionVerb(method));
            }
        }
        assertThat(actual)
                .as("agentic endpoint permission verbs must match the expected map")
                .isEqualTo(new TreeMap<>(EXPECTED_VERBS));
    }

    @Test
    void everyDistinctPermissionCodeIsSeededInTheAuthResourceSeed() {
        Path seed = repoRoot().resolve("dc3/dependencies/postgres/initdb/02-iot-dc3-auth.sql");
        Assumptions.assumeTrue(
                Files.exists(seed),
                "auth seed not found relative to the module; run through Maven from the repository root");
        String sql = read(seed);

        for (String verb : EXPECTED_VERBS.values().stream().distinct().sorted().toList()) {
            String resourceCode = SERVICE_NAME + ":" + verb;
            assertThat(sql)
                    .as("dc3_resource seed must contain '%s' so the permission is grantable", resourceCode)
                    .contains("'" + resourceCode + "'");
        }
    }

    @Test
    void seededScopeFlagsMatchTheVerbConvention() {
        Path seed = repoRoot().resolve("dc3/dependencies/postgres/initdb/02-iot-dc3-auth.sql");
        Assumptions.assumeTrue(
                Files.exists(seed),
                "auth seed not found relative to the module; run through Maven from the repository root");
        String sql = read(seed);
        Map<String, Integer> scopeFlagByVerb = Map.of("add", 0, "delete", 1, "update", 2, "list", 3, "get", 4);

        for (String verb : EXPECTED_VERBS.values().stream().distinct().sorted().toList()) {
            String resourceCode = SERVICE_NAME + ":" + verb;
            String scope = verb.substring(verb.lastIndexOf(':') + 1);
            Pattern row =
                    Pattern.compile(Pattern.quote("'" + resourceCode + "', '" + SERVICE_NAME + "', 6, ") + "(\\d+)");
            Matcher matcher = row.matcher(sql);
            assertThat(matcher.find())
                    .as("seed row for %s must be an API-type row", resourceCode)
                    .isTrue();
            assertThat(Integer.parseInt(matcher.group(1)))
                    .as("scope flag for %s", resourceCode)
                    .isEqualTo(scopeFlagByVerb.get(scope));
        }
    }

    private String permissionVerb(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).as("%s must carry @PreAuthorize", method).isNotNull();
        Matcher matcher = PERMISSION_CAN_PATTERN.matcher(preAuthorize.value());
        assertThat(matcher.find())
                .as("%s must use @perm.can(domain, scope)", method)
                .isTrue();
        return matcher.group(1) + ":" + matcher.group(2);
    }

    private boolean hasMapping(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
                || method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }

    private Path repoRoot() {
        // Robust across surefire (cwd = module dir) and IDE runs (cwd varies): walk up
        // from the working directory until the auth seed is visible.
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 6 && current != null; depth++) {
            if (Files.exists(current.resolve("dc3/dependencies/postgres/initdb/02-iot-dc3-auth.sql"))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of("").toAbsolutePath();
    }

    private String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }
}
