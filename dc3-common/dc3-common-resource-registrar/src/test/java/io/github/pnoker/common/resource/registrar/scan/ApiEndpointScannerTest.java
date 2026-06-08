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

import io.github.pnoker.common.facade.entity.bo.FacadeScannedApiBO;
import io.github.pnoker.common.resource.registrar.config.ResourceRegistrarProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    void unsupportedMethodsAndDefaultExcludesAreFilteredOut() {
        register(PatchOnlyController.class);
        register(ActuatorController.class);
        ApiEndpointScanner scanner = new ApiEndpointScanner(handlerMapping, new ResourceRegistrarProperties());

        List<FacadeScannedApiBO> apis = scanner.scan();

        assertThat(apis).isEmpty();
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
    @RequestMapping("/api/methodless")
    static class MethodLessController {
        @RequestMapping
        public String anything() {
            return "ok";
        }
    }
}
