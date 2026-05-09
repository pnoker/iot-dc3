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

package io.github.pnoker.common.constant.common;

/**
 * Environment variables and configuration variables related constants
 * <p>
 * Provides constants for environment types, Spring configuration properties, Eureka
 * configuration, and driver-related configuration keys.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public class EnvironmentConstant {

    /**
     * Development environment: "dev"
     */
    public static final String ENV_DEV = "dev";

    /**
     * Test environment: "test"
     */
    public static final String ENV_TEST = "test";

    /**
     * Pre-production environment: "pre"
     */
    public static final String ENV_PRE = "pre";

    /**
     * Production environment: "pro"
     */
    public static final String ENV_PRO = "pro";

    /**
     * Eureka TLS enable configuration key: "eureka.client.tls.enabled"
     */
    public static final String EUREKA_TLS_ENABLE = "eureka.client.tls.enabled";

    /**
     * Eureka service registry URL configuration key:
     * "eureka.client.service-url.defaultZone"
     */
    public static final String EUREKA_SERVICE_URL = "eureka.client.service-url.defaultZone";

    /**
     * Current environment type configuration key: "spring.env"
     */
    public static final String SPRING_ENV = "spring.env";

    /**
     * Current group configuration key: "spring.group"
     */
    public static final String SPRING_GROUP = "spring.group";

    /**
     * Application name configuration key: "spring.application.name"
     */
    public static final String SPRING_APPLICATION_NAME = "spring.application.name";

    /**
     * Driver tenant name configuration key: "dc3.driver.tenant"
     */
    public static final String DRIVER_TENANT = "dc3.driver.tenant";

    /**
     * Driver node configuration key: "dc3.driver.node"
     */
    public static final String DRIVER_NODE = "dc3.driver.node";

    /**
     * Driver service configuration key: "dc3.driver.service"
     */
    public static final String DRIVER_SERVICE = "dc3.driver.service";

    /**
     * Driver host configuration key: "dc3.driver.host"
     */
    public static final String DRIVER_HOST = "dc3.driver.host";

    /**
     * Driver client name configuration key: "dc3.driver.client"
     */
    public static final String DRIVER_CLIENT = "dc3.driver.client";

    /**
     * Driver port configuration key: "dc3.driver.port"
     */
    public static final String DRIVER_PORT = "dc3.driver.port";

    /**
     * Driver MQTT client name configuration key: "dc3.driver.mqtt.client"
     */
    public static final String MQTT_CLIENT = "dc3.driver.mqtt.client";

    /**
     * Driver MQTT topic prefix configuration key: "dc3.driver.mqtt.topic-prefix"
     */
    public static final String MQTT_PREFIX = "dc3.driver.mqtt.topic-prefix";

    private EnvironmentConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}
