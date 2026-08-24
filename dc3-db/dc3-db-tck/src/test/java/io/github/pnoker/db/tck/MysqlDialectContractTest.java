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

package io.github.pnoker.db.tck;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * MySQL harness — mysql:8.4 with the derived seed mounted at the entrypoint's
 * initdb directory. The connection selects the dc3_data database; cross-
 * database references (dc3_manager.*) resolve as on the compose deployment.
 *
 * @author pnoker
 * @since 2026.8.24
 */
@Testcontainers(disabledWithoutDocker = true)
class MysqlDialectContractTest extends AbstractDbDialectContractTest {

    /**
     * Plain container with the compose-proven recipe (the stock MySQLContainer
     * module's startup arguments conflict with 8.4 here): env-driven init,
     * seed at the entrypoint's initdb directory, readiness via mysqladmin.
     */
    @Container
    private static final GenericContainer<?> MYSQL =
            new GenericContainer<>("mysql:8.4")
                    .withEnv("MYSQL_ROOT_PASSWORD", "dc3")
                    .withEnv("MYSQL_USER", "dc3")
                    .withEnv("MYSQL_PASSWORD", "dc3")
                    .withEnv("MYSQL_DATABASE", "dc3_data")
                    .withExposedPorts(3306)
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("../../dc3/dependencies/mysql/initdb"),
                            "/docker-entrypoint-initdb.d")
                    // log-following is unreliable against podman; the listening
                    // TCP port flips exactly when the server is ready
                    .waitingFor(Wait.forListeningPort()
                            .withStartupTimeout(java.time.Duration.ofMinutes(5)));

    @Override
    protected String jdbcUrl(String database) {
        return "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306)
                + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    @Override
    protected String databaseId() {
        return "mysql";
    }

    @Override
    protected String driverClass() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    protected String username() {
        // the seeded dc3 user only holds dc3_data; the TCK spans four databases
        return "root";
    }

    @Override
    protected String password() {
        return "dc3";
    }
}
