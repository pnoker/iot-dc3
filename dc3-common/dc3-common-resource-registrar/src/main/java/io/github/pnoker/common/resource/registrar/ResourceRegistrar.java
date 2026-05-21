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

package io.github.pnoker.common.resource.registrar;

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.facade.api.ResourceRegistryFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeResourceRegistrySyncCommandBO;
import io.github.pnoker.common.facade.entity.bo.FacadeResourceRegistrySyncResultBO;
import io.github.pnoker.common.facade.entity.bo.FacadeScannedApiBO;
import io.github.pnoker.common.resource.registrar.config.ResourceRegistrarProperties;
import io.github.pnoker.common.resource.registrar.scan.ApiEndpointScanner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Drives a one-shot sync of the local HTTP endpoint inventory to the auth resource tables
 * as soon as the application finishes starting. The transport is selected by whichever
 * {@link ResourceRegistryFacade} bean is active: local facade for single-JVM deployments
 * or gRPC facade for distributed center services.
 *
 * @author pnoker
 * @version 2025.9.0
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
     * Create a registrar that scans the current service and submits the inventory through
     * the active facade implementation.
     *
     * @param scanner     endpoint scanner for the local WebFlux mappings
     * @param facade      transport-neutral resource registry facade
     * @param properties  registrar runtime options
     * @param environment Spring environment used for service-name fallback
     */

    /**
     * Register scanned endpoints after the application is ready and all WebFlux handler
     * mappings have been built. Failures abort startup only when
     * {@code dc3.resource-registrar.fail-fast=true}; otherwise the service keeps running
     * and logs the registration error.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        if (!properties.isEnabled()) {
            log.info("Resource registrar disabled; skipping endpoint registration");
            return;
        }
        String serviceName = resolveServiceName();
        if (StringUtils.isBlank(serviceName)) {
            String msg = "Resource registrar cannot resolve a service name "
                    + "(set dc3.resource-registrar.service-name or spring.application.name)";
            if (properties.isFailFast()) {
                throw new IllegalStateException(msg);
            }
            log.warn(msg);
            return;
        }
        try {
            List<FacadeScannedApiBO> apis = scanner.scan();
            FacadeResourceRegistrySyncCommandBO command = FacadeResourceRegistrySyncCommandBO.builder()
                    .serviceName(serviceName)
                    .deleteMissing(properties.isDeleteMissing())
                    .apis(apis)
                    .build();
            FacadeResourceRegistrySyncResultBO result = facade.sync(command);
            log.info(
                    "Resource registrar synced {} endpoints for [{}]: inserted={}, updated={}, deleted={}, unchanged={}",
                    apis.size(), serviceName, result.getInserted(), result.getUpdated(), result.getDeleted(),
                    result.getUnchanged());
        } catch (RuntimeException e) {
            if (properties.isFailFast()) {
                throw e;
            }
            log.error("Resource registrar failed to sync endpoints for [{}]", serviceName, e);
        }
    }

    /**
     * Resolve the stable service name used as the namespace for generated API codes.
     */
    private String resolveServiceName() {
        String name = properties.getServiceName();
        return StringUtils.isBlank(name) ? environment.getProperty("spring.application.name") : name;
    }

}
