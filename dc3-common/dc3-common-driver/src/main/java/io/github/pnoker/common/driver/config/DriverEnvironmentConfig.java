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

package io.github.pnoker.common.driver.config;

import io.github.pnoker.common.constant.common.EnvironmentConstant;
import io.github.pnoker.common.utils.EnvironmentUtil;
import io.github.pnoker.common.utils.HostUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring environment post-processor that derives and injects runtime driver properties
 * such as node, service, host, and client identifiers.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class DriverEnvironmentConfig implements EnvironmentPostProcessor {

    /**
     * Registers legacy {@code driver.*} → {@code dc3.*} property aliases and adds
     * the driver node/service/host/client identifiers to the {@link ConfigurableEnvironment}.
     *
     * @param environment the Spring environment being customized
     * @param application the current Spring application
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        addLegacyDriverAliases(environment);

        String node = environment.getProperty(EnvironmentConstant.DRIVER_NODE, String.class);
        if (StringUtils.isEmpty(node)) {
            node = EnvironmentUtil.getNodeId();
        }

        String tenant = environment.getProperty(EnvironmentConstant.DRIVER_TENANT, String.class);
        String name = environment.getProperty(EnvironmentConstant.SPRING_APPLICATION_NAME, String.class);
        String client = MessageFormat.format("{0}/{1}/{2}", tenant, name, node);
        String service = MessageFormat.format("{0}/{1}", tenant, name);

        Map<String, Object> source = new HashMap<>(4);
        source.put(EnvironmentConstant.DRIVER_NODE, node);
        source.put(EnvironmentConstant.DRIVER_SERVICE, service);
        source.put(EnvironmentConstant.DRIVER_HOST, HostUtil.localHost());
        source.put(EnvironmentConstant.DRIVER_CLIENT, client);
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("driver", source));
    }

    private void addLegacyDriverAliases(ConfigurableEnvironment environment) {
        Map<String, Object> aliases = new HashMap<>();
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
                for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                    if (propertyName.startsWith("driver.")) {
                        String aliasName = "dc3." + propertyName;
                        if (!environment.containsProperty(aliasName)) {
                            aliases.put(aliasName, enumerablePropertySource.getProperty(propertyName));
                        }
                    }
                }
            }
        }
        if (!aliases.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource("legacyDriverAliases", aliases));
        }
    }

}
