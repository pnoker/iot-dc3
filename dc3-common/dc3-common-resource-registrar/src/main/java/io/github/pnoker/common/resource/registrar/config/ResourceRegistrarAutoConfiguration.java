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

package io.github.pnoker.common.resource.registrar.config;

import io.github.pnoker.common.facade.api.ResourceRegistryFacade;
import io.github.pnoker.common.resource.registrar.ResourceRegistrar;
import io.github.pnoker.common.resource.registrar.scan.ApiEndpointScanner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

/**
 * Auto-configuration for the resource registrar. Registers when WebFlux is on the
 * classpath, a {@link ResourceRegistryFacade} bean exists (contributed by either the
 * local or gRPC facade impl), and the feature is not disabled via properties.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.5
 */
@AutoConfiguration
@EnableConfigurationProperties(ResourceRegistrarProperties.class)
@ConditionalOnClass(RequestMappingHandlerMapping.class)
@ConditionalOnProperty(prefix = "dc3.resource-registrar", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResourceRegistrarAutoConfiguration {

    /**
     * Scanner bean backed by the application WebFlux mapping registry. It is shared by
     * whichever registrar instance is created for the active facade mode.
     */
    @Bean
    public ApiEndpointScanner apiEndpointScanner(RequestMappingHandlerMapping requestMappingHandlerMapping,
                                                 ResourceRegistrarProperties properties) {
        return new ApiEndpointScanner(requestMappingHandlerMapping, properties);
    }

    /**
     * Create the startup registrar only when a facade implementation is active. This keeps
     * services that include the scanner but do not include a local or gRPC auth facade from
     * failing auto-configuration.
     */
    @Bean
    @ConditionalOnBean(ResourceRegistryFacade.class)
    public ResourceRegistrar resourceRegistrar(ApiEndpointScanner scanner, ResourceRegistryFacade facade,
                                               ResourceRegistrarProperties properties, Environment environment) {
        return new ResourceRegistrar(scanner, facade, properties, environment);
    }

}
