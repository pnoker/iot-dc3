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

package io.github.pnoker.common.sql;

import com.zaxxer.hikari.HikariDataSource;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractJdbcDriverCustomServiceTest {

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private HikariDataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    private TestJdbcDriver service;

    private static Map<String, AttributeBO> pointConfig(String writeQuery) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("writeQuery", AttributeBO.builder().value(writeQuery).type(AttributeTypeEnum.STRING).build());
        return m;
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
    void setUp() {
        service = new TestJdbcDriver(driverSenderService);
        service.initial();
        // Pre-populate the connection cache so write() reuses the mocked data source
        // instead of building a real HikariCP pool.
        service.connectMap.put(1L, dataSource);
    }

    @Test
    void writeBindsValueAsParameterRatherThanInterpolating() throws Exception {
        String writeQuery = "UPDATE sensor SET v = ? WHERE id = 1";
        String maliciousValue = "1'); DROP TABLE sensor; --";
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(writeQuery)).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        Boolean result = service.write(new HashMap<>(), pointConfig(writeQuery), device(1L), point(1L),
                WritePointValue.builder().value(maliciousValue).type(PointTypeEnum.STRING).build());

        assertThat(result).isTrue();
        // The query reaches the driver verbatim — the value is never concatenated into it.
        verify(connection).prepareStatement(writeQuery);
        // The value is bound as a positional parameter, neutralising SQL injection.
        verify(preparedStatement).setString(1, maliciousValue);
    }

    /**
     * Minimal concrete subclass so the abstract JDBC driver can be exercised in isolation.
     */
    private static class TestJdbcDriver extends AbstractJdbcDriverCustomService {

        TestJdbcDriver(DriverSenderService driverSenderService) {
            super(driverSenderService);
        }

        @Override
        protected String buildJdbcUrl(Map<String, AttributeBO> driverConfig) {
            return "jdbc:test:mem";
        }

        @Override
        protected String getDriverClassName() {
            return "org.test.Driver";
        }

        @Override
        protected int getDefaultPort() {
            return 0;
        }
    }

}
