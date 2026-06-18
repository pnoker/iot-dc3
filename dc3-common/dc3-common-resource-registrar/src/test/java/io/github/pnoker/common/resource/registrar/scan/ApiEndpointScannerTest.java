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

import io.github.pnoker.common.annotation.PublicEndpoint;
import io.github.pnoker.common.facade.entity.bo.FacadeScannedApiBO;
import io.github.pnoker.common.resource.registrar.config.ResourceRegistrarProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiEndpointScannerTest {

    private final RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();

    @BeforeEach
    void setUp() {
        handlerMapping.setApplicationContext(new org.springframework.context.support.StaticApplicationContext());
        handlerMapping.afterPropertiesSet();
    }

    @Test
    void scansBasicCrudControllerIntoFourEndpoints() {
        register(SampleController.class);
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, new ResourceRegistrarProperties());

        List<FacadeScannedApiBO> apis = scanner.scan();

        assertThat(apis).extracting(FacadeScannedApiBO::getMethod, FacadeScannedApiBO::getPath)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("GET", "/api/sample/{id}"),
                        org.assertj.core.groups.Tuple.tuple("POST", "/api/sample"),
                        org.assertj.core.groups.Tuple.tuple("PUT", "/api/sample/{id}"),
                        org.assertj.core.groups.Tuple.tuple("DELETE", "/api/sample/{id}"));
        assertThat(apis).allSatisfy(api -> {
            assertThat(api.getApiGroup()).isEqualTo("SampleController");
            assertThat(api.getApiName()).matches("sample:(get|add|update|delete)");
        });
    }

    @Test
    void operationAnnotationDrivesTitleAndRemarkWithMethodNameFallback() {
        register(OperationController.class);
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, new ResourceRegistrarProperties());

        List<FacadeScannedApiBO> apis = scanner.scan();

        FacadeScannedApiBO created = apis.stream()
                .filter(a -> "POST".equals(a.getMethod()))
                .findFirst().orElseThrow();
        assertThat(created.getTitle()).isEqualTo("Add Device");
        assertThat(created.getRemark()).isEqualTo("Create a new device record");

        FacadeScannedApiBO fetched = apis.stream()
                .filter(a -> "GET".equals(a.getMethod()))
                .findFirst().orElseThrow();
        // No @Operation: title falls back to the handler method name, remark stays blank.
        assertThat(fetched.getTitle()).isEqualTo("get");
        assertThat(fetched.getRemark()).isEmpty();
    }

    @Test
    void xDc3AiExtensionPopulatesAiMetadataWithBlankDefaults() {
        register(McpAnnotatedController.class);
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, new ResourceRegistrarProperties());

        List<FacadeScannedApiBO> apis = scanner.scan();

        FacadeScannedApiBO command = apis.stream()
                .filter(a -> "/api/mcp/command".equals(a.getPath()))
                .findFirst().orElseThrow();
        assertThat(command.getRiskLevel()).isEqualTo("HIGH");
        assertThat(command.getOpenWorldHint()).isEqualTo("true");
        assertThat(command.getHidden()).isEqualTo("true");
        // Attributes left at their annotation defaults stay blank, so derivation still applies.
        assertThat(command.getDestructiveHint()).isEmpty();
        assertThat(command.getIdempotentHint()).isEmpty();
        assertThat(command.getAiDescription()).isEmpty();

        FacadeScannedApiBO plain = apis.stream()
                .filter(a -> "/api/mcp/plain".equals(a.getPath()))
                .findFirst().orElseThrow();
        // No x-dc3-ai extension: every AI metadata field is blank so the catalog derives them.
        assertThat(plain.getRiskLevel()).isEmpty();
        assertThat(plain.getDestructiveHint()).isEmpty();
        assertThat(plain.getOpenWorldHint()).isEmpty();
        assertThat(plain.getIdempotentHint()).isEmpty();
        assertThat(plain.getAiDescription()).isEmpty();
        assertThat(plain.getHidden()).isEmpty();
    }

    @Test
    void unsupportedMethodsAndDefaultExcludesAreFilteredOut() {
        register(PatchOnlyController.class);
        register(ActuatorController.class);
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, new ResourceRegistrarProperties());

        List<FacadeScannedApiBO> apis = scanner.scan();

        assertThat(apis).isEmpty();
    }

    @Test
    void mcpToolsPathIsNotDefaultExcluded() {
        register(McpToolsController.class);
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, new ResourceRegistrarProperties());

        List<FacadeScannedApiBO> apis = scanner.scan();

        assertThat(apis).extracting(FacadeScannedApiBO::getMethod, FacadeScannedApiBO::getPath)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("GET", "/mcp_tools"));
    }

    @Test
    void userExcludePatternsAreApplied() {
        register(SampleController.class);
        ResourceRegistrarProperties props = new ResourceRegistrarProperties();
        props.setExcludePaths(List.of("/api/sample/*"));
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, props);

        List<FacadeScannedApiBO> apis = scanner.scan();

        assertThat(apis).extracting(FacadeScannedApiBO::getPath)
                .containsExactly("/api/sample");
    }

    @Test
    void duplicateMethodAndPathAcrossControllersIsDeduplicated() {
        register(SampleController.class);
        register(DuplicateController.class);
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, new ResourceRegistrarProperties());

        List<FacadeScannedApiBO> apis = scanner.scan();

        long getMatches = apis.stream()
                .filter(a -> "GET".equals(a.getMethod()) && "/api/sample/{id}".equals(a.getPath()))
                .count();
        assertThat(getMatches).isEqualTo(1);
    }

    @Test
    void multipleHttpMethodsOnOneHandlerFanOut() {
        register(MultiMethodController.class);
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, new ResourceRegistrarProperties());

        List<FacadeScannedApiBO> apis = scanner.scan();

        assertThat(apis).extracting(FacadeScannedApiBO::getMethod, FacadeScannedApiBO::getPath)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("GET", "/api/multi"),
                        org.assertj.core.groups.Tuple.tuple("POST", "/api/multi"));
    }

    @Test
    void mappingWithoutHttpMethodIsSkipped() {
        register(MethodLessController.class);
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, new ResourceRegistrarProperties());

        assertThat(scanner.scan()).isEmpty();
    }

    @Test
    void preAuthorizePermissionIsUsedAsApiNameWhenPresent() {
        register(ProjectConventionController.class);
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, new ResourceRegistrarProperties());

        List<FacadeScannedApiBO> apis = scanner.scan();

        assertThat(apis).extracting(FacadeScannedApiBO::getMethod, FacadeScannedApiBO::getPath,
                        FacadeScannedApiBO::getApiName)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("GET", "/api/convention/get_by_id", "convention:get"),
                        org.assertj.core.groups.Tuple.tuple("POST", "/api/convention/list", "convention:list"),
                        org.assertj.core.groups.Tuple.tuple("POST", "/api/convention/update", "convention:update"),
                        org.assertj.core.groups.Tuple.tuple("POST", "/api/convention/delete", "convention:delete"));
    }

    @Test
    void publicEndpointIsExcludedFromRegistration() {
        register(PublicTokenController.class);
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, new ResourceRegistrarProperties());

        assertThat(scanner.scan()).isEmpty();
    }

    private void register(Class<?> controllerClass) {
        try {
            Object bean = controllerClass.getDeclaredConstructor().newInstance();
            // detectHandlerMethods is protected on the abstract base
            java.lang.reflect.Method detect = handlerMapping.getClass().getSuperclass().getSuperclass()
                    .getDeclaredMethod("detectHandlerMethods", Object.class);
            detect.setAccessible(true);
            detect.invoke(handlerMapping, bean);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @RestController
    @RequestMapping("/api/sample")
    static class SampleController {
        @GetMapping("/{id}")
        public String get(String id) {
            return id;
        }

        @PostMapping
        public String create() {
            return "ok";
        }

        @PutMapping("/{id}")
        public String update(String id) {
            return id;
        }

        @DeleteMapping("/{id}")
        public String delete(String id) {
            return id;
        }
    }

    @RestController
    @RequestMapping("/api/op")
    static class OperationController {
        @Operation(summary = "Add Device", description = "Create a new device record")
        @PostMapping
        public String create() {
            return "ok";
        }

        @GetMapping("/{id}")
        public String get(String id) {
            return id;
        }
    }

    @RestController
    @RequestMapping("/api/mcp")
    static class McpAnnotatedController {
        @Operation(summary = "Issue Command", extensions = @Extension(name = "x-dc3-ai", properties = {
                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                @ExtensionProperty(name = "openWorld", value = "true"),
                @ExtensionProperty(name = "hidden", value = "true")
        }))
        @PostMapping("/command")
        public String command() {
            return "ok";
        }

        @PostMapping("/plain")
        public String plain() {
            return "ok";
        }
    }

    @RestController
    @RequestMapping("/api/duplicate")
    static class DuplicateController {
        @GetMapping("/api/sample/{id}")
        public String collide(String id) {
            return id;
        }
    }

    @RestController
    @RequestMapping("/api/multi")
    static class MultiMethodController {
        @RequestMapping(method = {org.springframework.web.bind.annotation.RequestMethod.GET,
                org.springframework.web.bind.annotation.RequestMethod.POST})
        public String both() {
            return "ok";
        }
    }

    @RestController
    @RequestMapping("/api/patch")
    static class PatchOnlyController {
        @PatchMapping
        public String patch() {
            return "ok";
        }
    }

    @RestController
    static class ActuatorController {
        @GetMapping("/actuator/health")
        public String health() {
            return "UP";
        }
    }

    @RestController
    static class McpToolsController {
        @GetMapping("/mcp_tools")
        public String tools() {
            return "ok";
        }
    }

    @RestController
    @RequestMapping("/api/methodless")
    static class MethodLessController {
        @RequestMapping
        public String anything() {
            return "ok";
        }
    }

    @RestController
    @RequestMapping("/api/convention")
    static class ProjectConventionController {
        @PreAuthorize("@perm.can('convention', 'get')")
        @GetMapping("/get_by_id")
        public String getById() {
            return "ok";
        }

        @PreAuthorize("@perm.can('convention', 'list')")
        @PostMapping("/list")
        public String list() {
            return "ok";
        }

        @PreAuthorize("@perm.can('convention', 'update')")
        @PostMapping("/update")
        public String update() {
            return "ok";
        }

        @PreAuthorize("@perm.can('convention', 'delete')")
        @PostMapping("/delete")
        public String delete() {
            return "ok";
        }
    }

    @RestController
    @RequestMapping("/token")
    static class PublicTokenController {
        @PublicEndpoint
        @PostMapping("/generate")
        public String generate() {
            return "ok";
        }
    }
}
