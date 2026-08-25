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

import io.github.pnoker.common.auth.mapper.OAuthMcpMapper;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.data.mapper.PointValueMapper;
import io.github.pnoker.common.manager.mapper.DriverLeaseMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dialect-neutral relational contract suite (docs/design/storage-abstraction.md
 * §3.3): one set of mapper-level assertions runs against PostgreSQL and MySQL 8
 * with identical fixtures. The suite exercises the real mapper XML — including
 * the databaseId-routed forks — so passing it certifies that both dialects
 * deliver the same behavior on the forked statements: the fenced latest-value
 * upsert, the state upsert + re-select shape, the three-step expired-lease
 * claim, the lease upserts' row-local increments, the revision triggers, and
 * the tool-catalog JSON extraction.
 *
 * @author pnoker
 * @since 2026.8.24
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDbDialectContractTest {

    private static long idSeq = System.currentTimeMillis();

    /**
     * JDBC url of the engine under test for the given database (seed loaded).
     */
    protected abstract String jdbcUrl(String database);

    protected abstract String username();

    protected abstract String password();

    /**
     * MyBatis databaseId for the engine under test: "postgres" or "mysql".
     */
    protected abstract String databaseId();

    /**
     * JDBC driver class for the engine under test.
     */
    protected abstract String driverClass();

    /**
     * One factory per database, mirroring the production @DS wiring: the mappers
     * under test span four databases (dc3_data state, dc3_history latest,
     * dc3_manager leases, dc3_auth catalog); table names stay unqualified and
     * resolve against the connection's selected database/schema.
     */
    private final java.util.Map<String, SqlSessionFactory> factories = new java.util.HashMap<>();

    private SqlSessionFactory factoryFor(String database) {
        return factories.computeIfAbsent(database, name -> {
            org.apache.ibatis.datasource.unpooled.UnpooledDataSource dataSource =
                    new org.apache.ibatis.datasource.unpooled.UnpooledDataSource(
                            driverClass(), jdbcUrl(name), username(), password());
            org.apache.ibatis.session.Configuration configuration =
                    new org.apache.ibatis.session.Configuration();
            configuration.setEnvironment(new Environment("tck-" + name, new JdbcTransactionFactory(), dataSource));
            // the harness knows its engine — set the databaseId the VendorDatabaseIdProvider
            // would resolve, routing <statement databaseId="..."> forks in the mapper XML
            configuration.setDatabaseId(databaseId());
            configuration.setMapUnderscoreToCamelCase(true);
            registerDialectHandlers(configuration);
            for (String xml : List.of("mapping/EntityStateMapper.xml", "mapping/PointValueMapper.xml",
                    "mapping/DriverLeaseMapper.xml", "mapping/OAuthMcpMapper.xml")) {
                try {
                    // affectData is a mybatis-plus DTD extension (cache eviction hint);
                    // the vanilla parser rejects it and the TCK does not depend on it
                    String text = new String(org.apache.ibatis.io.Resources.getResourceAsStream(xml).readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8).replace(" affectData=\"true\"", "");
                    new org.apache.ibatis.builder.xml.XMLMapperBuilder(
                            new java.io.ByteArrayInputStream(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            configuration, xml, configuration.getSqlFragments()).parse();
                } catch (Exception e) {
                    throw new IllegalStateException("failed to parse " + xml, e);
                }
            }
            return new SqlSessionFactoryBuilder().build(configuration);
        });
    }

    /**
     * Engine-specific type handlers (e.g. PostgreSQL timestamptz bridging).
     */
    protected void registerDialectHandlers(org.apache.ibatis.session.Configuration configuration) {
    }

    private SqlSession open(String database) {
        return factoryFor(database).openSession(true);
    }

    private static long freshId() {
        return ++idSeq;
    }

    /**
     * The platform time convention: DATETIME(6) stored and compared in UTC.
     */
    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(java.time.ZoneOffset.UTC);
    }

    // ===== 1) fenced latest-value upsert (PointValueMapper.upsertLatestBatch) =====

    @Test
    void fencedLatestUpsertKeepsTheWinningEnvelope() {
        try (SqlSession session = open("dc3_history")) {
            PointValueMapper mapper = session.getMapper(PointValueMapper.class);
            long tenant = freshId(), device = freshId(), point = freshId();
            mapper.upsertLatestBatch(List.of(latest(tenant, device, point, "m-1", 5L, 1)));
            mapper.upsertLatestBatch(List.of(latest(tenant, device, point, "m-2", 9L, 2)));
            // stale fencing must not overwrite
            mapper.upsertLatestBatch(List.of(latest(tenant, device, point, "m-old", 3L, 3)));
            var rows = mapper.selectLatestPointValues(tenant, device, List.of(point));
            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().getMessageId()).isEqualTo("m-2");
            assertThat(rows.getFirst().getFencingToken()).isEqualTo(9L);
        }
    }

    @Test
    void fencedLatestTiebreaksOnTimeThenSequenceThenMessageId() {
        try (SqlSession session = open("dc3_history")) {
            PointValueMapper mapper = session.getMapper(PointValueMapper.class);
            long tenant = freshId(), device = freshId(), point = freshId();
            LocalDateTime time = LocalDateTime.of(2026, 8, 24, 12, 0, 0);
            // equal fencing + time: later sequence wins
            mapper.upsertLatestBatch(List.of(latest(tenant, device, point, "a", 7L, time, 1L)));
            mapper.upsertLatestBatch(List.of(latest(tenant, device, point, "b", 7L, time, 2L)));
            var rows = mapper.selectLatestPointValues(tenant, device, List.of(point));
            assertThat(rows.getFirst().getMessageId()).isEqualTo("b");
            // equal fencing + time + sequence: later message id wins
            mapper.upsertLatestBatch(List.of(latest(tenant, device, point, "c", 7L, time, 2L)));
            rows = mapper.selectLatestPointValues(tenant, device, List.of(point));
            assertThat(rows.getFirst().getMessageId()).isEqualTo("c");
        }
    }

    // ===== 2) entity-state upsert + re-select (the RETURNING-decoupled shape) =====

    @Test
    void stateUpsertInsertsThenBumpsLeaseVersionAndReSelects() {
        try (SqlSession session = open("dc3_data")) {
            EntityStateMapper mapper = session.getMapper(EntityStateMapper.class);
            long tenant = freshId(), entity = freshId();
            mapper.upsertEntityState(freshId(), tenant, (byte) 1, entity, 0L, (byte) 1, (byte) 0,
                    nowUtc().plusSeconds(60), 60, (byte) 0, "tck", null);
            var first = mapper.selectByUniqueKey(tenant, (byte) 1, entity);
            assertThat(first).isNotNull();
            assertThat(first.getLeaseVersion()).isEqualTo(1);

            mapper.upsertEntityState(freshId(), tenant, (byte) 1, entity, 0L, (byte) 2, (byte) 0,
                    nowUtc().plusSeconds(60), 60, (byte) 0, "tck", null);
            var second = mapper.selectByUniqueKey(tenant, (byte) 1, entity);
            assertThat(second.getLeaseVersion()).isEqualTo(2);
            assertThat(second.getLastStateFlag()).isEqualTo((byte) 1);
        }
    }

    // ===== 3) three-step expired-lease claim =====

    @Test
    void expiredClaimLocksFlipsAndDerives() {
        try (SqlSession session = open("dc3_data")) {
            EntityStateMapper mapper = session.getMapper(EntityStateMapper.class);
            long tenant = freshId(), entity = freshId();
            mapper.upsertEntityState(freshId(), tenant, (byte) 1, entity, 0L, (byte) 2, (byte) 0,
                    nowUtc().minusSeconds(5), 60, (byte) 0, "tck", null);
            var locked = mapper.selectExpiredForClaim((byte) 1, (byte) 2, (byte) 3, (byte) 0, 10);
            assertThat(locked).anyMatch(row -> row.getEntityId().equals(entity));
            mapper.markClaimedOffline(locked.stream().map(
                            io.github.pnoker.common.data.entity.model.EntityStateDO::getId).toList(),
                    (byte) 0, 120);
            var flipped = mapper.selectByUniqueKey(tenant, (byte) 1, entity);
            assertThat(flipped.getStateFlag()).isEqualTo((byte) 0);
            assertThat(flipped.getLastStateFlag()).isEqualTo((byte) 2);
            assertThat(flipped.getLeaseVersion()).isEqualTo(2);
        }
    }

    // ===== 4) lease upserts: row-local increments =====

    @Test
    void deviceLeaseFencingAdvancesOnlyOnOwnershipChange() {
        try (SqlSession session = open("dc3_manager")) {
            DriverLeaseMapper mapper = session.getMapper(DriverLeaseMapper.class);
            long tenant = freshId(), device = freshId();
            mapper.upsertDeviceLeases(List.of(lease(tenant, 11, device, "node-a")));
            var afterInsert = leaseRow(session, tenant, device);
            assertThat(afterInsert.fencingToken()).isEqualTo(1L);
            // same owner re-asserting: token must not move
            mapper.upsertDeviceLeases(List.of(lease(tenant, 11, device, "node-a")));
            assertThat(leaseRow(session, tenant, device).fencingToken()).isEqualTo(1L);
            // ownership change: token advances
            mapper.upsertDeviceLeases(List.of(lease(tenant, 12, device, "node-b")));
            var afterMove = leaseRow(session, tenant, device);
            assertThat(afterMove.driverId()).isEqualTo(12L);
            assertThat(afterMove.fencingToken()).isEqualTo(2L);
        }
    }

    @Test
    void leaseStateAssignmentVersionAdvancesPerUpsert() {
        try (SqlSession session = open("dc3_manager")) {
            DriverLeaseMapper mapper = session.getMapper(DriverLeaseMapper.class);
            long tenant = freshId(), driver = freshId();
            mapper.upsertLeaseState(tenant, driver, "hash-1", 1L);
            var first = mapper.selectLeaseState(tenant, driver);
            assertThat(first.getAssignmentVersion()).isEqualTo(1L);
            mapper.upsertLeaseState(tenant, driver, "hash-2", 2L);
            var second = mapper.selectLeaseState(tenant, driver);
            assertThat(second.getAssignmentVersion()).isEqualTo(2L);
            assertThat(second.getMembershipHash()).isEqualTo("hash-2");
        }
    }

    // ===== 5) revision trigger (row-level on both engines) =====

    @Test
    void deviceChangesBumpTheOwningDriversRevision() throws Exception {
        try (SqlSession session = open("dc3_manager")) {
            var jdbc = session.getConnection();
            long tenant = freshId(), driver = freshId(), device = freshId();
            insertDevice(jdbc, device, driver, tenant);
            assertThat(revision(jdbc, tenant, driver)).isEqualTo(1L);
            // trivial update: no bump
            updateDeviceName(jdbc, device, "renamed");
            assertThat(revision(jdbc, tenant, driver)).isEqualTo(1L);
            // meaningful update: bump
            flipDeviceEnabled(jdbc, device);
            assertThat(revision(jdbc, tenant, driver)).isEqualTo(2L);
        }
    }

    // ===== 6) tool-catalog JSON extraction (seed data) =====

    @Test
    void toolCatalogCandidatesResolveTitlesThroughTheEncodedContent() throws Exception {
        // the catalog rows are runtime-registered (ApiEndpointScanner), not seeded —
        // plant one api row plus its permission resource and require both dialects
        // to surface the title through the encoded content double-hop
        long apiId = freshId();
        long resourceId = freshId();
        String content = "{\"title\":\"Tck Title\",\"url\":\"/tck\"}";
        // api_ext.content holds the inner object as an escaped JSON string
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"");
        String apiExt = "{\"type\":null,\"content\":\"" + escaped + "\",\"version\":1}";
        try (SqlSession session = open("dc3_auth")) {
            var jdbc = session.getConnection();
            // parameterized: the value must not travel through SQL text, where
            // MySQL's default backslash escapes would mangle the embedded JSON
            try (var ps = jdbc.prepareStatement("INSERT INTO dc3_api (id, service_name, api_type_flag, api_name, "
                    + "api_code, api_group, api_ext, enable_flag, remark, creator_id, creator_name, operator_id, "
                    + "operator_name, deleted) VALUES (?, 'tck-center', 0, 'tck:list', 'tck-center:POST:/tck/list', "
                    + "'', ?, 0, '', 1, 'tck', 1, 'tck', 0)")) {
                ps.setLong(1, apiId);
                // PG strictly rejects varchar->json and MySQL rejects the binary
                // binding Types.OTHER produces — branch on the engine under test
                if ("postgres".equals(databaseId())) {
                    ps.setObject(2, apiExt, java.sql.Types.OTHER);
                } else {
                    ps.setString(2, apiExt);
                }
                ps.executeUpdate();
            }
            exec(jdbc, "INSERT INTO dc3_resource (id, parent_resource_id, resource_name, resource_code, "
                    + "service_name, resource_type_flag, resource_scope_flag, entity_id, resource_ext, "
                    + "enable_flag, creator_id, creator_name, operator_id, operator_name, deleted) "
                    + "VALUES (" + resourceId + ", 0, 'tck api', 'tck-center:POST:/tck/list', 'tck-center', 6, 3, "
                    + apiId + ", '{}', 0, 1, 'tck', 1, 'tck', 0)");
        }
        try (SqlSession session = open("dc3_auth")) {
            OAuthMcpMapper mapper = session.getMapper(OAuthMcpMapper.class);
            var candidates = mapper.listRegistryToolCandidates();
            assertThat(candidates).anySatisfy(candidate -> {
                assertThat(candidate.getToolId()).isEqualTo("tck-center:POST:/tck/list");
                assertThat(candidate.getToolTitle()).isEqualTo("Tck Title");
            });
        }
    }

    private void exec(java.sql.Connection connection, String sql) {
        try (var ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ===== helpers =====

    private io.github.pnoker.common.data.entity.model.PointValueDO latest(
            long tenant, long device, long point, String messageId, long fencing, int sequence) {
        return latest(tenant, device, point, messageId, fencing,
                LocalDateTime.of(2026, 8, 24, 12, 0, sequence), (long) sequence);
    }

    private io.github.pnoker.common.data.entity.model.PointValueDO latest(
            long tenant, long device, long point, String messageId, long fencing,
            LocalDateTime createTime, Long sequence) {
        io.github.pnoker.common.data.entity.model.PointValueDO valueDO =
                new io.github.pnoker.common.data.entity.model.PointValueDO();
        valueDO.setTenantId(tenant);
        valueDO.setDeviceId(device);
        valueDO.setPointId(point);
        valueDO.setMessageId(messageId);
        valueDO.setSchemaVersion(1);
        valueDO.setDriverNode("tck-node");
        valueDO.setSequence(sequence);
        valueDO.setFencingToken(fencing);
        valueDO.setRawValue("raw");
        valueDO.setCalValue("cal");
        valueDO.setNumValue(1.0);
        valueDO.setDriverId(1L);
        valueDO.setCreateTime(createTime);
        valueDO.setOperateTime(createTime);
        return valueDO;
    }

    private record LeaseRow(long driverId, long fencingToken) {
    }

    private LeaseRow leaseRow(SqlSession session, long tenant, long device) {
        try (var ps = session.getConnection().prepareStatement(
                "SELECT driver_id, fencing_token FROM dc3_device_lease WHERE tenant_id=? AND device_id=?")) {
            ps.setLong(1, tenant);
            ps.setLong(2, device);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return new LeaseRow(rs.getLong(1), rs.getLong(2));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private io.github.pnoker.common.manager.entity.model.DeviceLeaseDO lease(
            long tenant, long driver, long device, String node) {
        io.github.pnoker.common.manager.entity.model.DeviceLeaseDO leaseDO =
                new io.github.pnoker.common.manager.entity.model.DeviceLeaseDO();
        leaseDO.setTenantId(tenant);
        leaseDO.setDriverId(driver);
        leaseDO.setDeviceId(device);
        leaseDO.setOwnerNode(node);
        return leaseDO;
    }

    private void insertDevice(java.sql.Connection connection, long id, long driverId, long tenantId)
            throws Exception {
        try (var ps = connection.prepareStatement(
                "INSERT INTO dc3_device (id, device_name, device_code, driver_id, profile_id, enable_flag, "
                        + "tenant_id, creator_id, creator_name, operator_id, operator_name) "
                        + "VALUES (?,?,?,?,?,0,?,1,'tck',1,'tck')")) {
            ps.setLong(1, id);
            ps.setString(2, "tck-device");
            ps.setString(3, "tck-" + id);
            ps.setLong(4, driverId);
            ps.setLong(5, 1);
            ps.setLong(6, tenantId);
            ps.executeUpdate();
        }
    }

    private void updateDeviceName(java.sql.Connection connection, long id, String name) throws Exception {
        try (var ps = connection.prepareStatement("UPDATE dc3_device SET device_name=? WHERE id=?")) {
            ps.setString(1, name);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private void flipDeviceEnabled(java.sql.Connection connection, long id) throws Exception {
        try (var ps = connection.prepareStatement("UPDATE dc3_device SET enable_flag=1 WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private long revision(java.sql.Connection connection, long tenantId, long driverId) throws Exception {
        try (var ps = connection.prepareStatement(
                "SELECT revision FROM dc3_driver_device_revision WHERE tenant_id=? AND driver_id=?")) {
            ps.setLong(1, tenantId);
            ps.setLong(2, driverId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }
}
