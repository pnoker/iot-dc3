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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for the gRPC facade implementation. Active by default
 * (when {@code dc3.facade.mode} is absent) or when explicitly set to
 * {@code grpc}. Scans the facade-grpc package so the {@code @Component} beans
 * are registered.
 *
 * @author pnoker
 * @since 2026.4.19
 */
@AutoConfiguration
@ConditionalOnProperty(name = "dc3.facade.mode", havingValue = "grpc", matchIfMissing = true)
@ComponentScan("io.github.pnoker.common.facade.grpc")
public class GrpcFacadeAutoConfiguration {
}
