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

import io.github.pnoker.common.constant.common.EnvironmentConstant;
import io.github.pnoker.common.utils.EnvironmentUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * RabbitMQ Environment Configuration
 * <p>
 * Environment post processor for configuring RabbitMQ constants based on development
 * environment and group tags. Used for multi-developer scenarios where different
 * exchanges, queues, and topics need to be isolated by environment and group identifiers.
 * </p>
 * <p>
 * The tag is stored as a system property ({@code dc3.rabbit.tag}) so that
 * {@link io.github.pnoker.common.constant.driver.RabbitConstant} can read it during its
 * static class initialization, allowing all fields to remain {@code final}.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Slf4j
@Order
public class RabbitmqEnvironmentConfig implements EnvironmentPostProcessor {

    /**
     * System property key used to communicate the environment/group tag to
     * {@link io.github.pnoker.common.constant.driver.RabbitConstant}'s static initializer.
     */
    public static final String DC3_RABBIT_TAG = "dc3.rabbit.tag";

    /**
     * Post-process environment to configure RabbitMQ constants
     *
     * @param environment ConfigurableEnvironment to modify
     * @param application SpringApplication instance
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // This configuration is used for multi-developer scenarios,
        // distinguishing different exchanges, queues, and topics based on env and group
        // identifiers
        String env = environment.getProperty(EnvironmentConstant.SPRING_ENV, String.class);
        String group = environment.getProperty(EnvironmentConstant.SPRING_GROUP, String.class);

        String tag = EnvironmentUtil.getTag(env, group);

        // Store tag as system property so RabbitConstant can read it during static
        // initialization. EnvironmentPostProcessor runs during prepareEnvironment(),
        // which is before application context creation -- so by the time any class
        // references RabbitConstant, the system property is already set.
        System.setProperty(DC3_RABBIT_TAG, tag);
        log.info("RabbitMQ environment tag set: {}", tag);
    }

}
