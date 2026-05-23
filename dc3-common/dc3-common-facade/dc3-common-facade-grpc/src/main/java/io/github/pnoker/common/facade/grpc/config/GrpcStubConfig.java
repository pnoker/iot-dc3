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

package io.github.pnoker.common.facade.grpc.config;

import io.github.pnoker.api.center.auth.ResourceRegistryApiGrpc;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.api.center.auth.TokenApiGrpc;
import io.github.pnoker.api.center.auth.UserApiGrpc;
import io.github.pnoker.api.center.auth.UserLoginApiGrpc;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.api.center.data.StatusHealthApiGrpc;
import io.github.pnoker.api.center.manager.CommandApiGrpc;
import io.github.pnoker.api.center.manager.DeviceApiGrpc;
import io.github.pnoker.api.center.manager.DriverApiGrpc;
import io.github.pnoker.api.center.manager.EventApiGrpc;
import io.github.pnoker.api.center.manager.PointApiGrpc;
import io.github.pnoker.api.center.manager.ProfileApiGrpc;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Configuration
public class GrpcStubConfig {

    @Bean
    public TenantApiGrpc.TenantApiBlockingStub tenantApiBlockingStub(GrpcChannelFactory channels) {
        return TenantApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    @Bean
    public UserApiGrpc.UserApiBlockingStub userApiBlockingStub(GrpcChannelFactory channels) {
        return UserApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    @Bean
    public TokenApiGrpc.TokenApiBlockingStub tokenApiBlockingStub(GrpcChannelFactory channels) {
        return TokenApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    @Bean
    public UserLoginApiGrpc.UserLoginApiBlockingStub userLoginApiBlockingStub(GrpcChannelFactory channels) {
        return UserLoginApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    @Bean
    public ResourceRegistryApiGrpc.ResourceRegistryApiBlockingStub resourceRegistryApiBlockingStub(
            GrpcChannelFactory channels) {
        return ResourceRegistryApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    @Bean
    public DriverApiGrpc.DriverApiBlockingStub managerDriverApiBlockingStub(GrpcChannelFactory channels) {
        return DriverApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    @Bean
    public DeviceApiGrpc.DeviceApiBlockingStub managerDeviceApiBlockingStub(GrpcChannelFactory channels) {
        return DeviceApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    @Bean
    public PointApiGrpc.PointApiBlockingStub managerPointApiBlockingStub(GrpcChannelFactory channels) {
        return PointApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    @Bean
    public ProfileApiGrpc.ProfileApiBlockingStub managerProfileApiBlockingStub(GrpcChannelFactory channels) {
        return ProfileApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    @Bean
    public CommandApiGrpc.CommandApiBlockingStub commandApiBlockingStub(GrpcChannelFactory channels) {
        return CommandApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    @Bean
    public EventApiGrpc.EventApiBlockingStub eventApiBlockingStub(GrpcChannelFactory channels) {
        return EventApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    @Bean
    public PointValueApiGrpc.PointValueApiBlockingStub pointValueApiBlockingStub(GrpcChannelFactory channels) {
        return PointValueApiGrpc.newBlockingStub(channels.createChannel(DataConstant.SERVICE_NAME));
    }

    @Bean
    public StatusHealthApiGrpc.StatusHealthApiBlockingStub statusHealthApiBlockingStub(GrpcChannelFactory channels) {
        return StatusHealthApiGrpc.newBlockingStub(channels.createChannel(DataConstant.SERVICE_NAME));
    }

}
