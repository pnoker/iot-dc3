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

package io.github.pnoker.common.manager.biz.impl;

import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.driver.GrpcDriverRegisterDTO;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointAttributeBuilder;
import io.github.pnoker.common.manager.service.DriverAttributeService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointAttributeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverRegisterServiceImplTest {

    @Mock
    private GrpcDriverBuilder grpcDriverBuilder;

    @Mock
    private GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;

    @Mock
    private GrpcPointAttributeBuilder grpcPointAttributeBuilder;

    @Mock
    private DriverService driverService;

    @Mock
    private DriverAttributeService driverAttributeService;

    @Mock
    private PointAttributeService pointAttributeService;

    @Mock
    private TenantFacade tenantFacade;

    @InjectMocks
    private DriverRegisterServiceImpl service;

    private DriverBO driverBO;
    private FacadeTenantBO tenant;
    private GrpcDriverRegisterDTO request;

    @BeforeEach
    void setUp() {
        driverBO = new DriverBO();
        driverBO.setServiceName("dc3-driver-modbus-tcp");

        tenant = new FacadeTenantBO();
        tenant.setId(100L);
        tenant.setTenantCode("default");

        request = GrpcDriverRegisterDTO.newBuilder()
                .setTenant("default")
                .setDriver(GrpcDriverDTO.newBuilder().build())
                .build();
    }

    @Test
    void registerDriverRejectsUnknownTenant() {
        when(tenantFacade.selectByCode("default")).thenReturn(null);
        assertThatThrownBy(() -> service.registerDriver(request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Tenant");
        verify(driverService, never()).add(any(DriverBO.class));
        verify(driverService, never()).update(any(DriverBO.class));
    }

    @Test
    void registerDriverInsertsWhenNotPreviouslyRegistered() {
        when(tenantFacade.selectByCode("default")).thenReturn(tenant);
        when(grpcDriverBuilder.buildBOByGrpcDTO(any(GrpcDriverDTO.class))).thenReturn(driverBO);
        when(driverService.selectByServiceName(eq("dc3-driver-modbus-tcp"), eq(100L)))
                .thenReturn(null, driverBO);

        DriverBO result = service.registerDriver(request);

        assertThat(result).isSameAs(driverBO);
        assertThat(driverBO.getTenantId()).isEqualTo(100L);
        verify(driverService).add(driverBO);
        verify(driverService, never()).update(any(DriverBO.class));
    }

    @Test
    void registerDriverUpdatesWhenAlreadyRegistered() {
        DriverBO existing = new DriverBO();
        existing.setId(42L);
        existing.setServiceName("dc3-driver-modbus-tcp");
        when(tenantFacade.selectByCode("default")).thenReturn(tenant);
        when(grpcDriverBuilder.buildBOByGrpcDTO(any(GrpcDriverDTO.class))).thenReturn(driverBO);
        when(driverService.selectByServiceName(eq("dc3-driver-modbus-tcp"), eq(100L)))
                .thenReturn(existing, driverBO);

        service.registerDriver(request);

        verify(driverService).update(driverBO);
        verify(driverService, never()).add(any(DriverBO.class));
        assertThat(driverBO.getId()).isEqualTo(42L);
    }
}
