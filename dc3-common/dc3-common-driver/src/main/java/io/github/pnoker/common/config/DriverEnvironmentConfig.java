/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.config;

import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.common.constant.common.EnvironmentConstant;
import io.github.pnoker.common.utils.EnvironmentUtil;
import io.github.pnoker.common.utils.HostUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for initializing the driver-specific environment properties during application startup.
 * This class implements the EnvironmentPostProcessor interface to manipulate environment properties before the
 * Spring application context is refreshed.
 * <p>
 * On execution, the class performs the following:
 * - Retrieves the driver node identifier from environment properties or generates a new one if not available.
 * - Constructs driver-specific properties such as client identifier and service name.
 * - Adds these environment properties to the application's property sources at the highest precedence.
 * <p>
 * Annotations:
 * - {@code @Slf4j}: Provides a logger instance for the class.
 * - {@code @Configuration}: Marks the class as a source of bean definitions.
 * - {@code @Order}: Specifies the precedence of this configuration relative to others, ensuring it is
 * executed at a specific point in the application's lifecycle.
 * <p>
 * The following environment properties are processed or generated:
 * - {@code driver.node}: Identifier for the driver node (retrieved or generated).
 * - {@code driver.tenant}: Name of the tenant for the driver.
 * - {@code spring.application.name}: Name of the application.
 * - {@code driver.service}: Constructed service name combining tenant and application name.
 * - {@code driver.client}: Constructed client identifier using tenant, application name, and driver node.
 * - {@code driver.host}: Host address of the application obtained via utility methods.
 * <p>
 * This configuration ensures that essential driver-specific properties are readily available across
 * the application lifecycle by injecting them into the environment property sources.
 */
@Slf4j
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class DriverEnvironmentConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String node = environment.getProperty(EnvironmentConstant.DRIVER_NODE, String.class);
        if (CharSequenceUtil.isEmpty(node)) {
            node = EnvironmentUtil.getNodeId();
        }

        String tenant = environment.getProperty(EnvironmentConstant.DRIVER_TENANT, String.class);
        String name = environment.getProperty(EnvironmentConstant.SPRING_APPLICATION_NAME, String.class);
        String client = CharSequenceUtil.format("{}/{}_{}", tenant, name, node);
        String service = CharSequenceUtil.format("{}/{}", tenant, name);

        Map<String, Object> source = new HashMap<>(4);
        source.put(EnvironmentConstant.DRIVER_NODE, node);
        source.put(EnvironmentConstant.DRIVER_SERVICE, service);
        source.put(EnvironmentConstant.DRIVER_HOST, HostUtil.localHost());
        source.put(EnvironmentConstant.DRIVER_CLIENT, client);
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("driver", source));
    }

}
