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

import io.github.pnoker.api.center.auth.LocalCredentialApiGrpc;
import io.github.pnoker.api.center.auth.McpRuntimeApiGrpc;
import io.github.pnoker.api.center.auth.PermissionApiGrpc;
import io.github.pnoker.api.center.auth.ResourceRegistryApiGrpc;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.api.center.auth.TokenApiGrpc;
import io.github.pnoker.api.center.auth.UserApiGrpc;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * @author pnoker
 * @since 2016.10.1
 */
@Configuration(proxyBeanMethods = false)
@Import({
    GrpcStubConfig.AuthGrpcStubConfig.class,
    GrpcStubConfig.ManagerGrpcStubConfig.class,
    GrpcStubConfig.DataGrpcStubConfig.class,
})
public class GrpcStubConfig {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "dc3.facade.auth.mode", havingValue = "grpc", matchIfMissing = true)
    static class AuthGrpcStubConfig {

        @Bean
        public TenantApiGrpc.TenantApiStub tenantApiStub(GrpcChannelFactory channels) {
            return TenantApiGrpc.newStub(channels.createChannel(AuthConstant.SERVICE_NAME));
        }

        @Bean
        public UserApiGrpc.UserApiStub userApiStub(GrpcChannelFactory channels) {
            return UserApiGrpc.newStub(channels.createChannel(AuthConstant.SERVICE_NAME));
        }

        @Bean
        public TokenApiGrpc.TokenApiStub tokenApiStub(GrpcChannelFactory channels) {
            return TokenApiGrpc.newStub(channels.createChannel(AuthConstant.SERVICE_NAME));
        }

        @Bean
        public LocalCredentialApiGrpc.LocalCredentialApiStub localCredentialApiStub(GrpcChannelFactory channels) {
            return LocalCredentialApiGrpc.newStub(channels.createChannel(AuthConstant.SERVICE_NAME));
        }

        @Bean
        public PermissionApiGrpc.PermissionApiStub permissionApiStub(GrpcChannelFactory channels) {
            return PermissionApiGrpc.newStub(channels.createChannel(AuthConstant.SERVICE_NAME));
        }

        @Bean
        public ResourceRegistryApiGrpc.ResourceRegistryApiStub resourceRegistryApiStub(GrpcChannelFactory channels) {
            return ResourceRegistryApiGrpc.newStub(channels.createChannel(AuthConstant.SERVICE_NAME));
        }

        @Bean
        public McpRuntimeApiGrpc.McpRuntimeApiStub mcpRuntimeApiStub(GrpcChannelFactory channels) {
            return McpRuntimeApiGrpc.newStub(channels.createChannel(AuthConstant.SERVICE_NAME));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "dc3.facade.manager.mode", havingValue = "grpc", matchIfMissing = true)
    static class ManagerGrpcStubConfig {

        @Bean
        public DriverApiGrpc.DriverApiStub managerDriverApiStub(GrpcChannelFactory channels) {
            return DriverApiGrpc.newStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
        }

        @Bean
        public DeviceApiGrpc.DeviceApiStub managerDeviceApiStub(GrpcChannelFactory channels) {
            return DeviceApiGrpc.newStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
        }

        @Bean
        public ProfileApiGrpc.ProfileApiStub managerProfileApiStub(GrpcChannelFactory channels) {
            return ProfileApiGrpc.newStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
        }

        @Bean
        public PointApiGrpc.PointApiStub managerPointApiStub(GrpcChannelFactory channels) {
            return PointApiGrpc.newStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
        }

        @Bean
        public CommandApiGrpc.CommandApiStub commandApiStub(GrpcChannelFactory channels) {
            return CommandApiGrpc.newStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
        }

        @Bean
        public EventApiGrpc.EventApiStub eventApiStub(GrpcChannelFactory channels) {
            return EventApiGrpc.newStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "dc3.facade.data.mode", havingValue = "grpc", matchIfMissing = true)
    static class DataGrpcStubConfig {

        @Bean
        public PointValueApiGrpc.PointValueApiStub pointValueApiStub(GrpcChannelFactory channels) {
            return PointValueApiGrpc.newStub(channels.createChannel(DataConstant.SERVICE_NAME));
        }

        @Bean
        public StatusHealthApiGrpc.StatusHealthApiStub statusHealthApiStub(GrpcChannelFactory channels) {
            return StatusHealthApiGrpc.newStub(channels.createChannel(DataConstant.SERVICE_NAME));
        }
    }
}
