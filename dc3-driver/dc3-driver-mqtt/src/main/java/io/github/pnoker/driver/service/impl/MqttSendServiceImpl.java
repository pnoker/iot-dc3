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
package io.github.pnoker.driver.service.impl;

import io.github.pnoker.driver.service.MqttSendService;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Paho-backed MQTT publisher. Replaces the legacy Spring Integration
 * {@code @MessagingGateway} whose supporting infrastructure was removed; publishing
 * is a thin fire-and-forget wrapper so writes degrade to an error log entry when
 * the broker is unreachable instead of failing driver startup.
 *
 * @author pnoker
 * @since 2026.9.17
 */
@Slf4j
@Service
public class MqttSendServiceImpl implements MqttSendService {

    private final String brokerHost;

    private final int brokerPort;

    private final String username;

    private final String password;

    private final String defaultTopic;

    private final int defaultQos;

    private volatile MqttClient client;

    /** mqtt send service impl. */
    public MqttSendServiceImpl(
            @Value("${MQTT_BROKER_HOST:localhost}") String brokerHost,
            @Value("${MQTT_BROKER_PORT:1883}") int brokerPort,
            @Value("${MQTT_USERNAME:}") String username,
            @Value("${MQTT_PASSWORD:}") String password,
            @Value("${dc3.driver.mqtt.default-topic:dc3/d/v/dc3-driver-mqtt_default}") String defaultTopic,
            @Value("${dc3.driver.mqtt.default-qos:2}") int defaultQos) {
        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.username = username;
        this.password = password;
        this.defaultTopic = defaultTopic;
        this.defaultQos = defaultQos;
    }

    @Override
    public void sendToMqtt(String data) {
        publish(defaultTopic, defaultQos, data);
    }

    @Override
    public void sendToMqtt(Integer qos, String data) {
        publish(defaultTopic, qos == null ? defaultQos : qos, data);
    }

    @Override
    public void sendToMqtt(String topic, String data) {
        publish(topic, defaultQos, data);
    }

    @Override
    public void sendToMqtt(String topic, Integer qos, String data) {
        publish(topic, qos == null ? defaultQos : qos, data);
    }

    private synchronized void publish(String topic, int qos, String data) {
        try {
            MqttClient current = client();
            MqttMessage message = new MqttMessage(data.getBytes(StandardCharsets.UTF_8));
            message.setQos(qos);
            current.publish(topic, message);
        } catch (MqttException error) {
            log.error("MQTT publish failed, topic={}, broker={}:{}", topic, brokerHost, brokerPort, error);
        }
    }

    private MqttClient client() throws MqttException {
        MqttClient current = client;
        if (current == null || !current.isConnected()) {
            if (current != null) {
                current.close();
            }
            current = new MqttClient(
                    "tcp://" + brokerHost + ":" + brokerPort, MqttClient.generateClientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }
            current.connect(options);
            client = current;
        }
        return current;
    }

    /** Close the shared MQTT client, disconnecting forcibly. */
    @PreDestroy
    public void shutdown() {
        MqttClient current = client;
        if (current != null) {
            try {
                current.disconnectForcibly();
                current.close();
            } catch (MqttException ignored) {
                // shutting down; nothing meaningful to recover
            }
        }
    }
}
