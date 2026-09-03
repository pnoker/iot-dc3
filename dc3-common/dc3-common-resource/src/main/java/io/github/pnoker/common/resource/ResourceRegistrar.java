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
package io.github.pnoker.common.resource;

import io.github.pnoker.common.facade.api.ResourceRegistryFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeResourceRegistrySyncCommandBO;
import io.github.pnoker.common.resource.config.ResourceRegistrarProperties;
import io.github.pnoker.common.resource.scan.ApiEndpointScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

/**
 * Drives a one-shot sync of the local HTTP endpoint inventory to the auth resource tables
 * as soon as the application finishes starting. The transport is selected by whichever
 * {@link ResourceRegistryFacade} bean is active: local facade for single-JVM deployments
 * or gRPC facade for distributed center services.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@RequiredArgsConstructor
public class ResourceRegistrar {

    private final ApiEndpointScanner scanner;

    private final ResourceRegistryFacade facade;

    private final ResourceRegistrarProperties properties;

    private final Environment environment;

    /**
     * Register scanned endpoints after the application is ready and all WebFlux handler
     * mappings have been built. Failures abort startup only when
     * {@code dc3.resource-registrar.fail-fast=true}; otherwise the service keeps running
     * and logs the registration error.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        register().subscribe(null, error -> {
            if (properties.isFailFast()) {
                throw reactor.core.Exceptions.propagate(error);
            }
            log.error("Resource registrar startup synchronization failed", error);
        });
    }

    /** Register the driver runtime and return its metadata snapshot. */
    public Mono<Void> register() {
        if (!properties.isEnabled()) {
            log.info("Resource registrar disabled; skipping endpoint registration");
            return Mono.empty();
        }
        String serviceName = resolveServiceName();
        if (StringUtils.isBlank(serviceName)) {
            String msg = "Resource registrar cannot resolve a service name "
                    + "(set dc3.resource-registrar.service-name or spring.application.name)";
            if (properties.isFailFast()) {
                return Mono.error(new IllegalStateException(msg));
            }
            log.warn("Resource registrar disabled, reason=serviceNameUnavailable");
            return Mono.empty();
        }
        return Mono.fromSupplier(scanner::scan)
                .flatMap(apis -> facade.sync(FacadeResourceRegistrySyncCommandBO.builder()
                                .serviceName(serviceName)
                                .deleteMissing(properties.isDeleteMissing())
                                .apis(apis)
                                .build())
                        .doOnNext(result -> log.info(
                                "Resource registrar synchronized, serviceName={}, endpointCount={}, inserted={}, updated={}, deleted={}, unchanged={}",
                                serviceName,
                                apis.size(),
                                result.getInserted(),
                                result.getUpdated(),
                                result.getDeleted(),
                                result.getUnchanged())))
                .then()
                .onErrorResume(error -> properties.isFailFast()
                        ? Mono.error(error)
                        : Mono.fromRunnable(() -> log.error(
                                "Resource registrar synchronization failed, serviceName={}", serviceName, error)));
    }

    /**
     * Resolve the stable service name used as the namespace for generated API codes.
     */
    private String resolveServiceName() {
        String name = properties.getServiceName();
        return StringUtils.isBlank(name) ? environment.getProperty("spring.application.name") : name;
    }
}
