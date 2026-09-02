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

import io.github.pnoker.common.facade.grpc.CommandGrpcFacade;
import io.github.pnoker.common.facade.grpc.DeviceGrpcFacade;
import io.github.pnoker.common.facade.grpc.DriverGrpcFacade;
import io.github.pnoker.common.facade.grpc.EventGrpcFacade;
import io.github.pnoker.common.facade.grpc.GrpcFacadeSupport;
import io.github.pnoker.common.facade.grpc.LocalCredentialGrpcFacade;
import io.github.pnoker.common.facade.grpc.McpRuntimeGrpcFacade;
import io.github.pnoker.common.facade.grpc.PermissionGrpcFacade;
import io.github.pnoker.common.facade.grpc.PointCommandGrpcFacade;
import io.github.pnoker.common.facade.grpc.PointGrpcFacade;
import io.github.pnoker.common.facade.grpc.PointValueGrpcFacade;
import io.github.pnoker.common.facade.grpc.ProfileGrpcFacade;
import io.github.pnoker.common.facade.grpc.ResourceRegistryGrpcFacade;
import io.github.pnoker.common.facade.grpc.StatusHealthGrpcFacade;
import io.github.pnoker.common.facade.grpc.TenantGrpcFacade;
import io.github.pnoker.common.facade.grpc.TokenGrpcFacade;
import io.github.pnoker.common.facade.grpc.UserGrpcFacade;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcCommandBuilder;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcDeviceBuilder;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcDriverBuilder;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcEventBuilder;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcLocalCredentialBuilder;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcPointBuilder;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcPointValueBuilder;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcProfileBuilder;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcTenantBuilder;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcUserBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the gRPC facade implementation. Transport selection is
 * domain-specific so a center can use local facades for its own domain and gRPC for
 * cross-domain calls without creating a self-referential channel.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@AutoConfiguration
@Import({
    GrpcStubConfig.class,
    GrpcFacadeAutoConfiguration.AuthGrpcFacadeConfiguration.class,
    GrpcFacadeAutoConfiguration.ManagerGrpcFacadeConfiguration.class,
    GrpcFacadeAutoConfiguration.DataGrpcFacadeConfiguration.class,
})
@ComponentScan(
        basePackages = "io.github.pnoker.common.facade.grpc",
        useDefaultFilters = false,
        includeFilters =
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                            GrpcFacadeSupport.class,
                            RequestIdGrpcClientInterceptor.class,
                            RequestIdGrpcServerInterceptor.class,
                        }))
@EnableConfigurationProperties(GrpcFacadeProperties.class)
public class GrpcFacadeAutoConfiguration {

    @ConditionalOnProperty(name = "dc3.facade.auth.mode", havingValue = "grpc", matchIfMissing = true)
    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
            basePackages = "io.github.pnoker.common.facade.grpc",
            useDefaultFilters = false,
            includeFilters =
                    @ComponentScan.Filter(
                            type = FilterType.ASSIGNABLE_TYPE,
                            classes = {
                                TenantGrpcFacade.class,
                                UserGrpcFacade.class,
                                TokenGrpcFacade.class,
                                LocalCredentialGrpcFacade.class,
                                ResourceRegistryGrpcFacade.class,
                                PermissionGrpcFacade.class,
                                McpRuntimeGrpcFacade.class,
                                FacadeGrpcTenantBuilder.class,
                                FacadeGrpcUserBuilder.class,
                                FacadeGrpcLocalCredentialBuilder.class,
                            }))
    static class AuthGrpcFacadeConfiguration {}

    @ConditionalOnProperty(name = "dc3.facade.manager.mode", havingValue = "grpc", matchIfMissing = true)
    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
            basePackages = "io.github.pnoker.common.facade.grpc",
            useDefaultFilters = false,
            includeFilters =
                    @ComponentScan.Filter(
                            type = FilterType.ASSIGNABLE_TYPE,
                            classes = {
                                DeviceGrpcFacade.class,
                                DriverGrpcFacade.class,
                                PointGrpcFacade.class,
                                ProfileGrpcFacade.class,
                                CommandGrpcFacade.class,
                                EventGrpcFacade.class,
                                FacadeGrpcDeviceBuilder.class,
                                FacadeGrpcDriverBuilder.class,
                                FacadeGrpcPointBuilder.class,
                                FacadeGrpcProfileBuilder.class,
                                FacadeGrpcCommandBuilder.class,
                                FacadeGrpcEventBuilder.class,
                            }))
    static class ManagerGrpcFacadeConfiguration {}

    @ConditionalOnProperty(name = "dc3.facade.data.mode", havingValue = "grpc", matchIfMissing = true)
    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
            basePackages = "io.github.pnoker.common.facade.grpc",
            useDefaultFilters = false,
            includeFilters =
                    @ComponentScan.Filter(
                            type = FilterType.ASSIGNABLE_TYPE,
                            classes = {
                                PointValueGrpcFacade.class,
                                PointCommandGrpcFacade.class,
                                StatusHealthGrpcFacade.class,
                                FacadeGrpcPointValueBuilder.class,
                            }))
    static class DataGrpcFacadeConfiguration {}
}
