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

package io.github.pnoker.common.init;

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import io.github.pnoker.common.mqtt.service.MqttReceiveService;
import io.github.pnoker.common.mqtt.service.MqttScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * MQTT Initialization Runner for DC3 IoT Platform. This class is responsible for
 * initializing MQTT services and configurations during application startup. It implements
 * ApplicationRunner to ensure MQTT services are properly set up when the application
 * starts.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = {"io.github.pnoker.common.mqtt"})
@EnableConfigurationProperties({MqttProperties.class})
@RequiredArgsConstructor
public class MqttInitRunner implements ApplicationRunner {

    private final MqttScheduleService mqttScheduleService;
    private final ObjectProvider<MqttReceiveService> mqttReceiveServiceProvider;

    /**
     * Creates a new MQTT initialization runner with the specified MQTT schedule service.
     *
     * @param mqttScheduleService The MQTT schedule service to be initialized
     */
    /**
     * Executes the MQTT initialization process when the application starts. This method
     * is called automatically by Spring Boot after the application context is loaded.
     *
     * @param args Application arguments passed during startup
     * @throws Exception If an error occurs during MQTT initialization
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (mqttReceiveServiceProvider.getIfAvailable() == null) {
            log.info("Skip MQTT batch scheduler because no MqttReceiveService bean is present");
            return;
        }
        mqttScheduleService.initial();
    }

}
