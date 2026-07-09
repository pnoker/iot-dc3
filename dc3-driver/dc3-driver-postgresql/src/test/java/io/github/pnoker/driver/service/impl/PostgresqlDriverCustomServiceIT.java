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

import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the PostgreSQL driver against a real PostgreSQL instance.
 * <p>
 * Exercises the JDBC read/write paths of {@code AbstractJdbcDriverCustomService} that the unit
 * tests cannot cover, and verifies end-to-end that a written point value is bound as a parameter
 * (a SQL-injection payload is stored verbatim and cannot drop the table).
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresqlDriverCustomServiceIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18.4"));

    private PostgresqlDriverCustomServiceImpl service;

    private static AttributeBO str(String value) {
        return AttributeBO.builder().value(value).type(AttributeTypeEnum.STRING).build();
    }

    private static AttributeBO intAttr(int value) {
        return AttributeBO.builder().value(String.valueOf(value)).type(AttributeTypeEnum.INT).build();
    }

    private static Map<String, AttributeBO> driverConfig() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("host", str(POSTGRES.getHost()));
        config.put("port", intAttr(POSTGRES.getFirstMappedPort()));
        config.put("database", str(POSTGRES.getDatabaseName()));
        config.put("username", str(POSTGRES.getUsername()));
        config.put("password", str(POSTGRES.getPassword()));
        return config;
    }

    private static DeviceBO device(Long id) {
        DeviceBO device = new DeviceBO();
        device.setId(id);
        return device;
    }

    private static PointBO point(Long id) {
        PointBO point = new PointBO();
        point.setId(id);
        return point;
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new PostgresqlDriverCustomServiceImpl(Mockito.mock(DriverSenderService.class));
        service.initial();
        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS sensor");
            st.execute("CREATE TABLE sensor (id INT PRIMARY KEY, v VARCHAR(255))");
            st.execute("INSERT INTO sensor (id, v) VALUES (1, '42')");
        }
    }

    @Test
    void readExecutesQueryAndReturnsStoredValue() {
        Map<String, AttributeBO> pointConfig = new HashMap<>();
        pointConfig.put("readQuery", str("SELECT v FROM sensor WHERE id = 1"));

        ReadPointValue result = service.read(driverConfig(), pointConfig, device(1L), point(1L));

        assertThat(result.getValue()).isEqualTo("42");
    }

    @Test
    void writeBindsValueAsParameterAndResistsSqlInjection() throws Exception {
        String maliciousValue = "1'); DROP TABLE sensor; --";
        Map<String, AttributeBO> pointConfig = new HashMap<>();
        pointConfig.put("writeQuery", str("UPDATE sensor SET v = ? WHERE id = 1"));

        Boolean result = service.write(driverConfig(), pointConfig, device(1L), point(1L),
                WritePointValue.builder().value(maliciousValue).type(PointTypeEnum.STRING).build());

        assertThat(result).isTrue();
        // The table must still exist and store the payload verbatim — the injection was neutralised.
        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT v FROM sensor WHERE id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo(maliciousValue);
        }
    }

}
