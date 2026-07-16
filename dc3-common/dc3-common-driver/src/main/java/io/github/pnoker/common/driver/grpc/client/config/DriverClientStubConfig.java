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

package io.github.pnoker.common.driver.grpc.client.config;

import io.github.pnoker.api.common.driver.DeviceApiGrpc;
import io.github.pnoker.api.common.driver.DriverApiGrpc;
import io.github.pnoker.api.common.driver.PointApiGrpc;
import io.github.pnoker.common.constant.service.ManagerConstant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * Spring configuration that creates the blocking gRPC stubs used by the driver SDK to
 * talk to the manager center.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Configuration
public class DriverClientStubConfig {

    /**
     * Provides a blocking stub for the driver API exposed by the manager center.
     *
     * @param channels gRPC channel factory used to create the manager service channel
     * @return blocking stub bound to the manager driver service
     */
    @Bean
    public DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub(GrpcChannelFactory channels) {
        return DriverApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    /**
     * Provides a blocking stub for the device API exposed by the manager center.
     *
     * @param channels gRPC channel factory used to create the manager service channel
     * @return blocking stub bound to the manager device service
     */
    @Bean
    public DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub(GrpcChannelFactory channels) {
        return DeviceApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    /**
     * Provides a blocking stub for the point API exposed by the manager center.
     *
     * @param channels gRPC channel factory used to create the manager service channel
     * @return blocking stub bound to the manager point service
     */
    @Bean
    public PointApiGrpc.PointApiBlockingStub pointApiBlockingStub(GrpcChannelFactory channels) {
        return PointApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

}
