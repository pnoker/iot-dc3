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
package io.github.pnoker.driver.mqtt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * MQTT broker connection and publish defaults for the MQTT driver. Broker
 * coordinates and credentials are sourced from the {@code MQTT_*} environment
 * variables through {@code application.yml}; an empty username disables MQTT
 * authentication.
 *
 * @author pnoker
 * @since 2026.9.22
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.driver.mqtt")
public class MqttProperties {

    /**
     * Broker hostname the outbound publisher connects to.
     */
    @NotBlank(message = "Broker host can't be blank")
    private String brokerHost = "localhost";

    /**
     * Broker TCP port the outbound publisher connects to.
     */
    @NotNull(message = "Broker port can't be null")
    @Min(value = 1, message = "Broker port must be greater than 0")
    private Integer brokerPort = 1883;

    /**
     * Broker username; blank disables MQTT authentication.
     */
    private String username = "";

    /**
     * Broker password; only used when the username is set.
     */
    private String password = "";

    /**
     * Topic used when a publish call does not specify one.
     */
    @NotBlank(message = "Default topic can't be blank")
    private String defaultTopic = "dc3/d/v/dc3-driver-mqtt_default";

    /**
     * QoS level used when a publish call does not specify one.
     */
    @NotNull(message = "Default QoS can't be null")
    @Min(value = 0, message = "Default QoS must be 0, 1 or 2")
    @Max(value = 2, message = "Default QoS must be 0, 1 or 2")
    private Integer defaultQos = 2;
}
