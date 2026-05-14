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
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Shared RabbitMQ container with management plugin enabled, matching the
 * publisher-confirms expectations declared by the production RabbitConfig.
 */
public final class RabbitContainer {

    private static final DockerImageName IMAGE = DockerImageName.parse("rabbitmq:3.13-management");

    private static final boolean REUSE_ENABLED = TestcontainersConfiguration.getInstance().environmentSupportsReuse();

    @SuppressWarnings("resource")
    private static final RabbitMQContainer INSTANCE = new RabbitMQContainer(IMAGE)
            .withReuse(REUSE_ENABLED);

    static {
        INSTANCE.start();
    }

    private RabbitContainer() {
    }

    public static RabbitMQContainer instance() {
        return INSTANCE;
    }

    /**
     * Wire the running broker into the Spring environment. Publisher confirms and
     * mandatory delivery are turned on to mirror production wiring.
     */
    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", INSTANCE::getHost);
        registry.add("spring.rabbitmq.port", INSTANCE::getAmqpPort);
        registry.add("spring.rabbitmq.username", INSTANCE::getAdminUsername);
        registry.add("spring.rabbitmq.password", INSTANCE::getAdminPassword);
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "correlated");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
    }
}
