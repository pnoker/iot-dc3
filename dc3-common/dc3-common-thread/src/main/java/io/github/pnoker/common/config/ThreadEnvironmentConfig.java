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

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps legacy {@code server.thread.*} keys to the canonical {@code dc3.thread.*}
 * namespace so external deployments can migrate gradually.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.9
 */
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class ThreadEnvironmentConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> aliases = new HashMap<>();
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
                for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                    if (propertyName.startsWith("server.thread.")) {
                        String aliasName = "dc3.thread." + propertyName.substring("server.thread.".length());
                        if (!environment.containsProperty(aliasName)) {
                            aliases.put(aliasName, enumerablePropertySource.getProperty(propertyName));
                        }
                    }
                }
            }
        }
        if (!aliases.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource("legacyThreadAliases", aliases));
        }
    }

}
