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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * @author pnoker
 * @since 2016.10.1
 */
@Configuration
public class GrpcStubConfig {

    /**
     * Create and configure the application-managed tenant api blocking stub.
     *
     * @param channels channels
     * @return tenant api blocking stub result
     */
    @Bean
    public TenantApiGrpc.TenantApiBlockingStub tenantApiBlockingStub(GrpcChannelFactory channels) {
        return TenantApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed user api blocking stub.
     *
     * @param channels channels
     * @return user api blocking stub result
     */
    @Bean
    public UserApiGrpc.UserApiBlockingStub userApiBlockingStub(GrpcChannelFactory channels) {
        return UserApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed token api blocking stub.
     *
     * @param channels channels
     * @return token api blocking stub result
     */
    @Bean
    public TokenApiGrpc.TokenApiBlockingStub tokenApiBlockingStub(GrpcChannelFactory channels) {
        return TokenApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed local credential api blocking stub.
     *
     * @param channels channels
     * @return local credential api blocking stub result
     */
    @Bean
    public LocalCredentialApiGrpc.LocalCredentialApiBlockingStub localCredentialApiBlockingStub(
            GrpcChannelFactory channels) {
        return LocalCredentialApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed resource registry api blocking stub.
     *
     * @param channels channels
     * @return resource registry api blocking stub result
     */
    @Bean
    public ResourceRegistryApiGrpc.ResourceRegistryApiBlockingStub resourceRegistryApiBlockingStub(
            GrpcChannelFactory channels) {
        return ResourceRegistryApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed permission api blocking stub.
     *
     * @param channels channels
     * @return permission api blocking stub result
     */
    @Bean
    public PermissionApiGrpc.PermissionApiBlockingStub permissionApiBlockingStub(GrpcChannelFactory channels) {
        return PermissionApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed mcp runtime api blocking stub.
     *
     * @param channels channels
     * @return mcp runtime api blocking stub result
     */
    @Bean
    public McpRuntimeApiGrpc.McpRuntimeApiBlockingStub mcpRuntimeApiBlockingStub(GrpcChannelFactory channels) {
        return McpRuntimeApiGrpc.newBlockingStub(channels.createChannel(AuthConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed manager driver api blocking stub.
     *
     * @param channels channels
     * @return manager driver api blocking stub result
     */
    @Bean
    public DriverApiGrpc.DriverApiBlockingStub managerDriverApiBlockingStub(GrpcChannelFactory channels) {
        return DriverApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed manager device api blocking stub.
     *
     * @param channels channels
     * @return manager device api blocking stub result
     */
    @Bean
    public DeviceApiGrpc.DeviceApiBlockingStub managerDeviceApiBlockingStub(GrpcChannelFactory channels) {
        return DeviceApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed manager point api blocking stub.
     *
     * @param channels channels
     * @return manager point api blocking stub result
     */
    @Bean
    public PointApiGrpc.PointApiBlockingStub managerPointApiBlockingStub(GrpcChannelFactory channels) {
        return PointApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed manager profile api blocking stub.
     *
     * @param channels channels
     * @return manager profile api blocking stub result
     */
    @Bean
    public ProfileApiGrpc.ProfileApiBlockingStub managerProfileApiBlockingStub(GrpcChannelFactory channels) {
        return ProfileApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed command api blocking stub.
     *
     * @param channels channels
     * @return command api blocking stub result
     */
    @Bean
    public CommandApiGrpc.CommandApiBlockingStub commandApiBlockingStub(GrpcChannelFactory channels) {
        return CommandApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed event api blocking stub.
     *
     * @param channels channels
     * @return event api blocking stub result
     */
    @Bean
    public EventApiGrpc.EventApiBlockingStub eventApiBlockingStub(GrpcChannelFactory channels) {
        return EventApiGrpc.newBlockingStub(channels.createChannel(ManagerConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed point value api blocking stub.
     *
     * @param channels channels
     * @return point value api blocking stub result
     */
    @Bean
    public PointValueApiGrpc.PointValueApiBlockingStub pointValueApiBlockingStub(GrpcChannelFactory channels) {
        return PointValueApiGrpc.newBlockingStub(channels.createChannel(DataConstant.SERVICE_NAME));
    }

    /**
     * Create and configure the application-managed status health api blocking stub.
     *
     * @param channels channels
     * @return status health api blocking stub result
     */
    @Bean
    public StatusHealthApiGrpc.StatusHealthApiBlockingStub statusHealthApiBlockingStub(GrpcChannelFactory channels) {
        return StatusHealthApiGrpc.newBlockingStub(channels.createChannel(DataConstant.SERVICE_NAME));
    }

}
