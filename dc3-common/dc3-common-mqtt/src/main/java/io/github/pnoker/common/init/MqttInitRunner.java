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

import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import io.github.pnoker.common.mqtt.service.MqttScheduleService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * Mqtt initialization runner
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Component
@ComponentScan(basePackages = {
        "io.github.pnoker.common.mqtt.*"
})
@EnableConfigurationProperties({MqttProperties.class})
public class MqttInitRunner implements ApplicationRunner {

    private final MqttScheduleService mqttScheduleService;

    public MqttInitRunner(MqttScheduleService mqttScheduleService) {
        this.mqttScheduleService = mqttScheduleService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        mqttScheduleService.initial();
    }
}
