/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * 驱动注册相关接口实现
 *
 * @author pnoker
 * @since 2022.1.0
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
            RegisterBO entityBO = buildRegisterBOByProperty();
            log.info("The driver information is: {}", JsonUtil.toJsonString(entityBO));
            driverClient.driverRegister(entityBO);
        } catch (Exception e) {
            log.error("Driver initialization failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * 构建驱动注册信息
     *
     * @return DriverRegisterBO
     */
    private RegisterBO buildRegisterBOByProperty() {
        DriverBO driverBO = new DriverBO();
        driverBO.setDriverName(driverProperties.getName());
        driverBO.setDriverCode(driverProperties.getCode());
        driverBO.setServiceName(driverProperties.getService());
        driverBO.setServiceHost(driverProperties.getHost());
        driverBO.setDriverTypeFlag(driverProperties.getType());
        driverBO.setRemark(driverProperties.getRemark());

        RegisterBO entityBO = new RegisterBO();
        entityBO.setDriver(driverBO);
        entityBO.setTenant(driverProperties.getTenant());
        entityBO.setClient(driverProperties.getClient());
        entityBO.setDriverAttributes(driverProperties.getDriverAttribute());
        entityBO.setPointAttributes(driverProperties.getPointAttribute());
        return entityBO;
    }

}
