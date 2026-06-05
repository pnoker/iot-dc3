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
 * Provides constants for environment types, Spring configuration properties,
 * and driver-related configuration keys.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
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
     * Shared HTTP client configuration prefix: "dc3.http.client"
     */
    public static final String HTTP_CLIENT_PREFIX = "dc3.http.client";

    /**
     * Shared HMAC configuration prefix: "dc3.auth.hmac"
     */
    public static final String AUTH_HMAC_PREFIX = "dc3.auth.hmac";

    /**
     * Shared HMAC secret configuration key: "dc3.auth.hmac.secret"
     */
    public static final String AUTH_HMAC_SECRET_PROPERTY = "dc3.auth.hmac.secret";

    /**
     * Shared HMAC secret environment variable: "AUTH_HMAC_SECRET"
     */
    public static final String AUTH_HMAC_SECRET_ENV = "AUTH_HMAC_SECRET";

    /**
     * PostgreSQL profile auto-activation configuration key: "dc3.postgres.auto-profile"
     */
    public static final String POSTGRES_AUTO_PROFILE = "dc3.postgres.auto-profile";

    /**
     * Spring profile name for PostgreSQL-enabled applications: "postgres"
     */
    public static final String POSTGRES_PROFILE = "postgres";

    /**
     * PostgreSQL host environment variable: "POSTGRES_HOST"
     */
    public static final String POSTGRES_HOST_ENV = "POSTGRES_HOST";

    /**
     * PostgreSQL port environment variable: "POSTGRES_PORT"
     */
    public static final String POSTGRES_PORT_ENV = "POSTGRES_PORT";

    /**
     * PostgreSQL database environment variable: "POSTGRES_DB"
     */
    public static final String POSTGRES_DB_ENV = "POSTGRES_DB";

    /**
     * PostgreSQL schema environment variable: "POSTGRES_SCHEMA"
     */
    public static final String POSTGRES_SCHEMA_ENV = "POSTGRES_SCHEMA";

    /**
     * PostgreSQL username environment variable: "POSTGRES_USERNAME"
     */
    public static final String POSTGRES_USERNAME_ENV = "POSTGRES_USERNAME";

    /**
     * PostgreSQL password environment variable: "POSTGRES_PASSWORD"
     */
    public static final String POSTGRES_PASSWORD_ENV = "POSTGRES_PASSWORD";

    /**
     * Repository profile auto-activation configuration key: "dc3.repository.auto-profile"
     */
    public static final String REPOSITORY_AUTO_PROFILE = "dc3.repository.auto-profile";

    /**
     * Spring profile name for repository-enabled applications: "repository"
     */
    public static final String REPOSITORY_PROFILE = "repository";

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
     * Driver profile auto-activation configuration key: "dc3.driver.auto-profile"
     */
    public static final String DRIVER_AUTO_PROFILE = "dc3.driver.auto-profile";

    /**
     * Spring profile name for driver applications: "driver"
     */
    public static final String DRIVER_PROFILE = "driver";

    /**
     * RabbitMQ profile auto-activation configuration key: "dc3.rabbitmq.auto-profile"
     */
    public static final String RABBITMQ_AUTO_PROFILE = "dc3.rabbitmq.auto-profile";

    /**
     * Spring profile name for RabbitMQ-enabled applications: "rabbitmq"
     */
    public static final String RABBITMQ_PROFILE = "rabbitmq";

    /**
     * Quartz profile auto-activation configuration key: "dc3.quartz.auto-profile"
     */
    public static final String QUARTZ_AUTO_PROFILE = "dc3.quartz.auto-profile";

    /**
     * Spring profile name for Quartz-enabled applications: "quartz"
     */
    public static final String QUARTZ_PROFILE = "quartz";

    /**
     * Spring profile name for Data center applications: "data"
     */
    public static final String DATA_PROFILE = "data";

    /**
     * Spring profile name for Auth center applications: "auth"
     */
    public static final String AUTH_PROFILE = "auth";

    /**
     * Spring profile name for Manager center applications: "manager"
     */
    public static final String MANAGER_PROFILE = "manager";

    /**
     * Spring profile name for Gateway applications: "gateway"
     */
    public static final String GATEWAY_PROFILE = "gateway";

    /**
     * Spring profile name for Agentic applications: "agentic"
     */
    public static final String AGENTIC_PROFILE = "agentic";

    /**
     * Spring profile name for Web (MVC) applications: "web"
     */
    public static final String WEB_PROFILE = "web";

    /**
     * Spring profile name for Thread pool applications: "thread"
     */
    public static final String THREAD_PROFILE = "thread";

    /**
     * Spring profile name for MQTT applications: "mqtt"
     */
    public static final String MQTT_PROFILE = "mqtt";

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
