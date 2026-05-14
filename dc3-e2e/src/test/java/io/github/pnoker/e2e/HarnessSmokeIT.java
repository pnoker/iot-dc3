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

package io.github.pnoker.e2e;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.github.pnoker.e2e.harness.BaseE2eIT;
import io.github.pnoker.e2e.harness.E2eStack;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the E2E harness contract: the stack starts cleanly, exposes a reachable
 * Postgres + RabbitMQ, and the lifecycle is idempotent.
 *
 * <p>Disabled by default. Run with {@code DC3_E2E=true mvn -pl dc3-e2e -am verify}.
 */
class HarnessSmokeIT extends BaseE2eIT {

    @Test
    void postgresIsReachableAndAcceptsSimpleQueries() throws Exception {
        try (java.sql.Connection conn = DriverManager.getConnection(
                E2eStack.postgresJdbcUrl(), E2eStack.postgresUsername(), E2eStack.postgresPassword());
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    void rabbitMqIsReachableAndAcceptsAmqpHandshake() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(E2eStack.rabbitHost());
        factory.setPort(E2eStack.rabbitAmqpPort());
        factory.setUsername(E2eStack.rabbitUsername());
        factory.setPassword(E2eStack.rabbitPassword());

        try (Connection conn = factory.newConnection()) {
            assertThat(conn.isOpen()).isTrue();
        }
    }

    @Test
    void startIsIdempotentAcrossRepeatedCalls() {
        E2eStack.start();
        E2eStack.start();
        assertThat(E2eStack.postgresContainer().isRunning()).isTrue();
        assertThat(E2eStack.rabbitContainer().isRunning()).isTrue();
    }
}
