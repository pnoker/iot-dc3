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

package io.github.pnoker.common.driver.service.impl;

import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.bo.RegisterBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.grpc.client.DriverClient;
import io.github.pnoker.common.driver.service.DriverRegisterService;
import io.github.pnoker.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Default {@link DriverRegisterService} implementation that builds the registration
 * payload from {@link io.github.pnoker.common.driver.entity.property.DriverProperties}
 * and submits it to the manager center.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverRegisterServiceImpl implements DriverRegisterService {

    /**
     * Driver configuration properties used to assemble the registration payload.
     */
    private final DriverProperties driverProperties;

    /**
     * gRPC client used to register the driver with the manager center.
     */
    private final DriverClient driverClient;

    /**
     * Registers this driver with the manager center using the configured driver
     * properties.
     */
    @Override
    public Mono<Void> initial() {
        return Mono.defer(() -> {
            RegisterBO entityBO = buildRegisterBOByProperty();
            log.debug("Driver registration prepared, serviceName={}, driverCode={}, tenantCode={}",
                    entityBO.getDriver().getServiceName(), entityBO.getDriver().getDriverCode(), entityBO.getTenant());
            return driverClient.driverRegister(entityBO);
        }).onErrorMap(error -> !(error instanceof ServiceException),
                error -> new ServiceException("Driver initialization failed: {}", error.getMessage(), error));
    }

    /**
     * Build driver registration information from the driver properties.
     *
     * @return {@link RegisterBO} the assembled driver registration business object
     */
    private RegisterBO buildRegisterBOByProperty() {
        // Create and populate driver business object with properties
        DriverBO driverBO = new DriverBO();
        driverBO.setDriverName(driverProperties.getName());
        driverBO.setDriverCode(driverProperties.getCode());
        driverBO.setServiceName(driverProperties.getService());
        driverBO.setServiceHost(driverProperties.getHost());
        driverBO.setDriverTypeFlag(driverProperties.getType());
        driverBO.setRemark(driverProperties.getRemark());

        // Create and populate registration business object
        RegisterBO entityBO = new RegisterBO();
        entityBO.setDriver(driverBO);
        entityBO.setTenant(driverProperties.getTenant());
        entityBO.setClient(driverProperties.getClient());
        entityBO.setNode(driverProperties.getNode());
        entityBO.setLeaseSeconds(driverProperties.getLease().getSeconds());
        entityBO.setDriverAttributes(driverProperties.getDriverAttribute());
        entityBO.setPointAttributes(driverProperties.getPointAttribute());
        entityBO.setCommandAttributes(driverProperties.getCommandAttribute());
        entityBO.setEventAttributes(driverProperties.getEventAttribute());
        return entityBO;
    }

}
