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
     * Driver tenant name configuration key: "driver.tenant"
     */
    public static final String DRIVER_TENANT = "driver.tenant";

    /**
     * Driver node configuration key: "driver.node"
     */
    public static final String DRIVER_NODE = "driver.node";

    /**
     * Driver service configuration key: "driver.service"
     */
    public static final String DRIVER_SERVICE = "driver.service";

    /**
     * Driver host configuration key: "driver.host"
     */
    public static final String DRIVER_HOST = "driver.host";

    /**
     * Driver client name configuration key: "driver.client"
     */
    public static final String DRIVER_CLIENT = "driver.client";

    /**
     * Driver port configuration key: "driver.port"
     */
    public static final String DRIVER_PORT = "driver.port";

    /**
     * Driver MQTT client name configuration key: "driver.mqtt.client"
     */
    public static final String MQTT_CLIENT = "driver.mqtt.client";

    /**
     * Driver MQTT topic prefix configuration key: "driver.mqtt.topic-prefix"
     */
    public static final String MQTT_PREFIX = "driver.mqtt.topic-prefix";

    private EnvironmentConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

}
