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

package io.github.pnoker.common.facade.local.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for the local facade implementation. Active only when
 * {@code dc3.facade.mode=local}. Scans the facade-local package so the
 * {@code @Component}/{@code @Mapper} beans are registered.
 *
 * @author pnoker
 * @since 2026.4.29
 */
@AutoConfiguration
@ConditionalOnProperty(name = "dc3.facade.mode", havingValue = "local")
@ComponentScan("io.github.pnoker.common.facade.local")
public class LocalFacadeAutoConfiguration {
}
