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
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DriverRegisterServiceImplTest {

    @Mock
    private DriverClient driverClient;

    private DriverProperties properties;
    private DriverRegisterServiceImpl service;

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    @BeforeEach
    void setUp() {
        properties = new DriverProperties();
        properties.setName("ModbusTcp");
        properties.setCode("modbus-tcp");
        properties.setService("dc3-driver-modbus-tcp");
        properties.setHost("127.0.0.1");
        properties.setTenant("default");
        properties.setClient("client-1");
        properties.setRemark("integration");
        properties.setType(DriverTypeFlagEnum.DRIVER_CLIENT);
        service = new DriverRegisterServiceImpl(properties, driverClient);
    }

    @Test
    void initialBuildsRegistrationPayloadFromProperties() {
        service.initial();

        ArgumentCaptor<RegisterBO> captor = ArgumentCaptor.forClass(RegisterBO.class);
        verify(driverClient).driverRegister(captor.capture());
        RegisterBO sent = captor.getValue();
        assertThat(sent.getTenant()).isEqualTo("default");
        assertThat(sent.getClient()).isEqualTo("client-1");
        DriverBO driver = sent.getDriver();
        assertThat(driver.getDriverName()).isEqualTo("ModbusTcp");
        assertThat(driver.getDriverCode()).isEqualTo("modbus-tcp");
        assertThat(driver.getServiceName()).isEqualTo("dc3-driver-modbus-tcp");
        assertThat(driver.getServiceHost()).isEqualTo("127.0.0.1");
        assertThat(driver.getDriverTypeFlag()).isEqualTo(DriverTypeFlagEnum.DRIVER_CLIENT);
    }

    @Test
    void initialWrapsClientFailureInServiceException() {
        doThrow(new RuntimeException("manager unavailable")).when(driverClient)
                .driverRegister(any());
        assertThatThrownBy(() -> service.initial())
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Driver initialization failed");
    }
}
