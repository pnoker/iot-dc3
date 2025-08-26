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
 * 环境变量, 配置变量 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class EnvironmentConstant {

    /**
     * 开发环境
     */
    public static final String ENV_DEV = "dev";
    /**
     * 测试环境
     */
    public static final String ENV_TEST = "test";
    /**
     * 预发布环境
     */
    public static final String ENV_PRE = "pre";
    /**
     * 生产环境
     */
    public static final String ENV_PRO = "pro";
    /**
     * 服务是否开启 Https
     */
    public static final String EUREKA_TLS_ENABLE = "eureka.client.tls.enabled";
    /**
     * Eureka 服务注册中心 Url
     */
    public static final String EUREKA_SERVICE_URL = "eureka.client.service-url.defaultZone";
    /**
     * 当前环境类型
     */
    public static final String SPRING_ENV = "spring.env";
    /**
     * 当前分组
     */
    public static final String SPRING_GROUP = "spring.group";
    /**
     * 应用名称
     */
    public static final String SPRING_APPLICATION_NAME = "spring.application.name";
    /**
     * 驱动租户名称
     */
    public static final String DRIVER_TENANT = "driver.tenant";
    /**
     * 驱动节点
     */
    public static final String DRIVER_NODE = "driver.node";
    /**
     * 驱动服务
     */
    public static final String DRIVER_SERVICE = "driver.service";
    /**
     * 驱动主机
     */
    public static final String DRIVER_HOST = "driver.host";
    /**
     * 驱动客户端名称
     */
    public static final String DRIVER_CLIENT = "driver.client";
    /**
     * 驱动端口
     */
    public static final String DRIVER_PORT = "driver.port";
    /**
     * 驱动 Mqtt 客户端名称
     */
    public static final String MQTT_CLIENT = "driver.mqtt.client";
    /**
     * 驱动 Mqtt Topic 前缀
     */
    public static final String MQTT_PREFIX = "driver.mqtt.topic-prefix";

    private EnvironmentConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
