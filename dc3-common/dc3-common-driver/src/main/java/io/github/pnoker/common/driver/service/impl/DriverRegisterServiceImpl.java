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
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation class responsible for driver registration services.
 * This service facilitates the initialization and registration of a driver
 * with the platform using the driver properties and the driver client.
 */
@Slf4j
@Service
public class DriverRegisterServiceImpl implements DriverRegisterService {

    @Resource
    private DriverProperties driverProperties;

    @Resource
    private DriverClient driverClient;

    @Override
    public void initial() {
        try {
            // Build driver registration information from properties
            RegisterBO entityBO = buildRegisterBOByProperty();
            // Log driver information for debugging
            log.info("The driver information is: {}", JsonUtil.toJsonString(entityBO));
            // Register driver with the driver client
            driverClient.driverRegister(entityBO);
        } catch (Exception e) {
            // Log error and exit if initialization fails
            log.error("Driver initialization failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * 构建驱动注册信息
     * Build driver registration information from properties
     *
     * @return DriverRegisterBO Driver registration business object
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
        entityBO.setDriverAttributes(driverProperties.getDriverAttribute());
        entityBO.setPointAttributes(driverProperties.getPointAttribute());
        return entityBO;
    }

}
