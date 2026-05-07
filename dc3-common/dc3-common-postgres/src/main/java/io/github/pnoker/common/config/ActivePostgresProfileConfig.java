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

package io.github.pnoker.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Active PostgreSQL Profile Configuration
 * <p>
 * Environment post processor configuration that automatically activates the PostgreSQL
 * profile for Spring Boot applications. This ensures PostgreSQL-specific configurations
 * are loaded with highest precedence.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ActivePostgresProfileConfig implements EnvironmentPostProcessor {

	@Override
	/**
	 * Post-process the Spring environment to activate PostgreSQL profile
	 * @param environment ConfigurableEnvironment to modify
	 * @param application SpringApplication instance
	 */
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		environment.addActiveProfile("postgres");
	}

}
