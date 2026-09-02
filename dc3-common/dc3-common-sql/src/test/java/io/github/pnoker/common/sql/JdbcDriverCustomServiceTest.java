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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JdbcDriverCustomServiceTest {

    @Mock
    private HikariDataSource dataSource;

    @Mock
    private HikariDataSource otherDataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private TestJdbcDriver service;

    private static AttributeBO str(String value) {
        return AttributeBO.builder().value(value).type(AttributeTypeEnum.STRING).build();
    }

    private static AttributeBO intAttr(String value) {
        return AttributeBO.builder().value(value).type(AttributeTypeEnum.INT).build();
    }

    private static Map<String, AttributeBO> readConfig(String readQuery) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("readQuery", str(readQuery));
        return m;
    }

    private static Map<String, AttributeBO> writeConfig(String writeQuery) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("writeQuery", str(writeQuery));
        return m;
    }

    private static Map<String, AttributeBO> driverConfig() {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("database", str("dc3"));
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

    private static PointBO point(Long id, RwTypeEnum rwFlag) {
        PointBO point = point(id);
        point.setRwFlag(rwFlag);
        return point;
    }

    private static WritePointValue writeValue(String value) {
        return WritePointValue.builder().value(value).type(PointTypeEnum.STRING).build();
    }

    @BeforeEach
    void setUp() {
        service = new TestJdbcDriver();
        service.initial();
    }

    // ---------- write path ----------

    @Test
    void writeBindsValueAsParameterRatherThanInterpolating() throws Exception {
        String writeQuery = "UPDATE sensor SET v = ? WHERE id = 1";
        String maliciousValue = "1'); DROP TABLE sensor; --";
        service.connectMap.put(1L, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(writeQuery)).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        Boolean result = service.write(
                driverConfig(), writeConfig(writeQuery), device(1L), point(1L), writeValue(maliciousValue));

        assertThat(result).isTrue();
        // The query reaches the driver verbatim — the value is never concatenated into it.
        verify(connection).prepareStatement(writeQuery);
        // The value is bound as a positional parameter, neutralising SQL injection.
        verify(preparedStatement).setString(1, maliciousValue);
        // queryTimeout is applied as a statement timeout, not a connection timeout.
        verify(preparedStatement).setQueryTimeout(30);
    }

    @Test
    void writeRejectsQueryWithoutExactlyOnePlaceholder() {
        service.connectMap.put(1L, dataSource);

        assertThatThrownBy(() -> service.write(
                        driverConfig(),
                        writeConfig("UPDATE sensor SET v = 'x' WHERE id = 1"),
                        device(1L),
                        point(1L),
                        writeValue("x")))
                .isInstanceOf(WritePointException.class);
        assertThatThrownBy(() -> service.write(
                        driverConfig(),
                        writeConfig("UPDATE sensor SET v = ?, w = ? WHERE id = 1"),
                        device(1L),
                        point(1L),
                        writeValue("x")))
                .isInstanceOf(WritePointException.class);

        // Configuration errors must not touch or invalidate the pool.
        verifyNoInteractions(dataSource);
        assertThat(service.connectMap).containsKey(1L);
    }

    @Test
    void writeFailureInvalidatesConnector() throws Exception {
        String writeQuery = "UPDATE sensor SET v = ? WHERE id = 1";
        service.connectMap.put(1L, dataSource);
        when(dataSource.getConnection()).thenThrow(new SQLException("boom"));
        when(dataSource.isClosed()).thenReturn(false);

        assertThatThrownBy(() ->
                        service.write(driverConfig(), writeConfig(writeQuery), device(1L), point(1L), writeValue("x")))
                .isInstanceOf(WritePointException.class);

        assertThat(service.connectMap).doesNotContainKey(1L);
        verify(dataSource).close();
    }

    // ---------- read path ----------

    @Test
    void readExecutesQueryAndReturnsFirstColumnOfFirstRow() throws Exception {
        String readQuery = "SELECT v FROM sensor WHERE id = 1";
        service.connectMap.put(1L, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(readQuery)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getObject(1)).thenReturn("42");

        ReadPointValue result = service.read(driverConfig(), readConfig(readQuery), device(1L), point(1L));

        assertThat(result.getValue()).isEqualTo("42");
        verify(preparedStatement).setQueryTimeout(30);
    }

    @Test
    void readReturnsNullWhenResultSetIsEmpty() throws Exception {
        String readQuery = "SELECT v FROM sensor WHERE id = 1";
        service.connectMap.put(1L, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(readQuery)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        ReadPointValue result = service.read(driverConfig(), readConfig(readQuery), device(1L), point(1L));

        assertThat(result.getValue()).isNull();
    }

    @Test
    void readReturnsNullWhenFirstColumnIsNull() throws Exception {
        String readQuery = "SELECT v FROM sensor WHERE id = 1";
        service.connectMap.put(1L, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(readQuery)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getObject(1)).thenReturn(null);

        ReadPointValue result = service.read(driverConfig(), readConfig(readQuery), device(1L), point(1L));

        assertThat(result.getValue()).isNull();
    }

    @Test
    void readRejectsQueryWithPlaceholder() {
        service.connectMap.put(1L, dataSource);

        assertThatThrownBy(() -> service.read(driverConfig(), readConfig("SELECT ?"), device(1L), point(1L)))
                .isInstanceOf(ReadPointException.class);

        verifyNoInteractions(dataSource);
        assertThat(service.connectMap).containsKey(1L);
    }

    @Test
    void readFailureInvalidatesConnector() throws Exception {
        String readQuery = "SELECT v FROM sensor WHERE id = 1";
        service.connectMap.put(1L, dataSource);
        when(dataSource.getConnection()).thenThrow(new SQLException("boom"));
        when(dataSource.isClosed()).thenReturn(false);

        assertThatThrownBy(() -> service.read(driverConfig(), readConfig(readQuery), device(1L), point(1L)))
                .isInstanceOf(ReadPointException.class);

        assertThat(service.connectMap).doesNotContainKey(1L);
        verify(dataSource).close();
    }

    // ---------- connector construction ----------

    @Test
    void missingRequiredDriverConfigThrowsConnectorExceptionAndCachesNothing() {
        assertThatThrownBy(() -> service.read(new HashMap<>(), readConfig("SELECT 1"), device(1L), point(1L)))
                .isInstanceOf(ConnectorException.class);

        assertThat(service.connectMap).isEmpty();
    }

    @Test
    void invalidIntegerConfigThrowsConnectorException() {
        Map<String, AttributeBO> config = driverConfig();
        config.put("queryTimeout", intAttr("abc"));

        assertThatThrownBy(() -> service.read(config, readConfig("SELECT 1"), device(1L), point(1L)))
                .isInstanceOf(ConnectorException.class);

        assertThat(service.connectMap).isEmpty();
    }

    // ---------- health ----------

    @Test
    void deviceHealthReturnsOfflineWhenDeviceIsNull() {
        assertThat(service.health(new HashMap<>(), null).getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
    }

    @Test
    void deviceHealthUsesConnectionValidity() throws Exception {
        service.connectMap.put(1L, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        assertThat(service.health(driverConfig(), device(1L)).getStatus()).isEqualTo(EntityStatusEnum.ONLINE);
    }

    @Test
    void deviceHealthReturnsOfflineWhenConnectionIsInvalid() throws Exception {
        service.connectMap.put(1L, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(false);

        assertThat(service.health(driverConfig(), device(1L)).getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
    }

    // ---------- metadata events ----------

    @Test
    void deviceDeleteEventClosesAndRemovesPool() throws Exception {
        service.connectMap.put(1L, dataSource);
        when(dataSource.isClosed()).thenReturn(false);

        service.event(new MetadataEventDTO(1L, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE));

        assertThat(service.connectMap).isEmpty();
        verify(dataSource).close();
    }

    @Test
    void deviceUpdateEventClosesAndRemovesPool() throws Exception {
        service.connectMap.put(1L, dataSource);
        when(dataSource.isClosed()).thenReturn(false);

        service.event(new MetadataEventDTO(1L, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE));

        assertThat(service.connectMap).isEmpty();
        verify(dataSource).close();
    }

    @Test
    void driverUpdateEventClosesAllPools() throws Exception {
        service.connectMap.put(1L, dataSource);
        service.connectMap.put(2L, otherDataSource);
        when(dataSource.isClosed()).thenReturn(false);
        when(otherDataSource.isClosed()).thenReturn(false);

        service.event(new MetadataEventDTO(null, MetadataTypeEnum.DRIVER, MetadataOperateTypeEnum.UPDATE));

        assertThat(service.connectMap).isEmpty();
        verify(dataSource).close();
        verify(otherDataSource).close();
    }

    @Test
    void pointEventDoesNotTouchPools() {
        service.connectMap.put(1L, dataSource);

        service.event(new MetadataEventDTO(1L, MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE));

        assertThat(service.connectMap).containsKey(1L);
        verify(dataSource, never()).close();
    }

    // ---------- lifecycle ----------

    @Test
    void initialKeepsExistingConnectorsWhenCalledAgain() {
        service.connectMap.put(1L, dataSource);

        service.initial();

        assertThat(service.connectMap).containsKey(1L);
    }

    // ---------- point validation ----------

    @Test
    void validatePointRequiresReadQueryWhenRwFlagIsUnknown() {
        ValidationReport report = service.validatePoint(new HashMap<>(), point(1L));

        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).isNotEmpty();
    }

    @Test
    void validatePointPassesForReadOnlyPointWithReadQuery() {
        ValidationReport report = service.validatePoint(readConfig("SELECT 1"), point(1L, RwTypeEnum.READ_ONLY));

        assertThat(report.isPassed()).isTrue();
    }

    @Test
    void validatePointRequiresReadQueryForReadOnlyPoint() {
        ValidationReport report =
                service.validatePoint(writeConfig("UPDATE t SET v = ?"), point(1L, RwTypeEnum.READ_ONLY));

        assertThat(report.isPassed()).isFalse();
    }

    @Test
    void validatePointPassesForWriteOnlyPointWithWriteQuery() {
        ValidationReport report =
                service.validatePoint(writeConfig("UPDATE t SET v = ?"), point(1L, RwTypeEnum.WRITE_ONLY));

        assertThat(report.isPassed()).isTrue();
    }

    @Test
    void validatePointRequiresWriteQueryForWriteOnlyPoint() {
        ValidationReport report = service.validatePoint(readConfig("SELECT 1"), point(1L, RwTypeEnum.WRITE_ONLY));

        assertThat(report.isPassed()).isFalse();
    }

    @Test
    void validatePointRequiresBothQueriesForReadWritePoint() {
        Map<String, AttributeBO> config = readConfig("SELECT 1");

        assertThat(service.validatePoint(config, point(1L, RwTypeEnum.READ_WRITE))
                        .isPassed())
                .isFalse();

        config.put("writeQuery", str("UPDATE t SET v = ?"));
        assertThat(service.validatePoint(config, point(1L, RwTypeEnum.READ_WRITE))
                        .isPassed())
                .isTrue();
    }

    @Test
    void validatePointRejectsWriteQueryWithoutExactlyOnePlaceholder() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("readQuery", str("SELECT 1"));
        config.put("writeQuery", str("UPDATE t SET v = 'x'"));

        ValidationReport report = service.validatePoint(config, point(1L, RwTypeEnum.READ_WRITE));

        assertThat(report.isPassed()).isFalse();
    }

    @Test
    void validatePointRejectsReadQueryWithPlaceholder() {
        ValidationReport report = service.validatePoint(readConfig("SELECT ?"), point(1L, RwTypeEnum.READ_ONLY));

        assertThat(report.isPassed()).isFalse();
    }

    /**
     * Minimal concrete subclass so the abstract JDBC driver can be exercised in isolation.
     */
    private static class TestJdbcDriver extends AbstractJdbcDriverCustomService {

        @Override
        protected String buildJdbcUrl(Map<String, AttributeBO> driverConfig) {
            return "jdbc:test:mem/" + getRequiredConfig(driverConfig, "database");
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
