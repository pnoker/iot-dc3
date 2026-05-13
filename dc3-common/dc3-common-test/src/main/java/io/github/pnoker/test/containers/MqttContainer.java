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

package io.github.pnoker.test.containers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Eclipse Mosquitto MQTT broker container for driver and common-mqtt tests.
 *
 * <p>Anonymous access on port 1883 is enabled to keep tests boilerplate-free; tests
 * that exercise authentication should override the configuration via the
 * {@link GenericContainer#withCopyToContainer copy mechanism} on a fresh instance.
 */
public final class MqttContainer {

    private static final DockerImageName IMAGE = DockerImageName.parse("eclipse-mosquitto:2.0");

    private static final String CONFIG = """
            listener 1883
            allow_anonymous true
            """;

    @SuppressWarnings("resource")
    private static final GenericContainer<?> INSTANCE = new GenericContainer<>(IMAGE)
            .withExposedPorts(1883)
            .withCopyToContainer(
                    org.testcontainers.images.builder.Transferable.of(CONFIG),
                    "/mosquitto/config/mosquitto.conf")
            .waitingFor(Wait.forLogMessage(".*mosquitto version.*\\n", 1))
            .withReuse(true);

    static {
        INSTANCE.start();
    }

    private MqttContainer() {
    }

    public static GenericContainer<?> instance() {
        return INSTANCE;
    }

    public static String brokerUrl() {
        return "tcp://%s:%d".formatted(INSTANCE.getHost(), INSTANCE.getMappedPort(1883));
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("dc3.driver.mqtt.url", MqttContainer::brokerUrl);
    }
}
