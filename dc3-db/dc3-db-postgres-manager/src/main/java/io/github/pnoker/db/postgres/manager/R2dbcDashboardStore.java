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
package io.github.pnoker.db.postgres.manager;

import io.github.pnoker.common.manager.repository.ReactiveDashboardStore;

import io.github.pnoker.common.manager.entity.bo.dashboard.BucketRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.DailyGrowthRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.ProfileBindingRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyDeviceRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyDriverRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyPointRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyProfileRow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
@ConditionalOnClass(DatabaseClient.class)
@RequiredArgsConstructor
public class R2dbcDashboardStore implements ReactiveDashboardStore {
    private static final String DRIVER = "dc3_manager.dc3_driver";
    private static final String DEVICE = "dc3_manager.dc3_device";
    private static final String PROFILE = "dc3_manager.dc3_profile";
    private static final String POINT = "dc3_manager.dc3_point";
    private final DatabaseClient databaseClient;

    @Override
    public Flux<BucketRow> countDriverByEnable(Long tenantId) {
        return bucketRows("enable_flag", DRIVER, tenantId, " GROUP BY enable_flag");
    }

    @Override
    public Flux<BucketRow> countDriverByType(Long tenantId) {
        return bucketRows("driver_type_flag", DRIVER, tenantId, " GROUP BY driver_type_flag");
    }

    @Override
    public Flux<BucketRow> countDriverByService(Long tenantId) {
        return bucketRows("service_name", DRIVER, tenantId, " GROUP BY service_name ORDER BY count DESC");
    }

    @Override
    public Flux<BucketRow> countDeviceByEnable(Long tenantId) {
        return bucketRows("enable_flag", DEVICE, tenantId, " GROUP BY enable_flag");
    }

    @Override
    public Flux<BucketRow> countDeviceByDriver(Long tenantId, int limit) {
        return bucketRows("driver_id", DEVICE, tenantId, " GROUP BY driver_id ORDER BY count DESC LIMIT " + limit);
    }

    @Override
    public Flux<BucketRow> countDeviceByProfile(Long tenantId, int limit) {
        return bucketRows(
                "profile_id",
                DEVICE,
                tenantId,
                " AND profile_id IS NOT NULL GROUP BY profile_id ORDER BY count DESC LIMIT " + limit);
    }

    private DatabaseClient.GenericExecuteSpec bucketSpec(String column, String table, Long tenantId, String suffix) {
        return databaseClient
                .sql("SELECT " + column + " AS bucket_key, COUNT(*) AS count FROM " + table
                        + " WHERE deleted=0 AND tenant_id=:tenant_id" + suffix)
                .bind("tenant_id", tenantId);
    }

    private Flux<BucketRow> bucketRows(String column, String table, Long tenantId, String suffix) {
        return bucketSpec(column, table, tenantId, suffix)
                .map((row, metadata) -> {
                    BucketRow value = new BucketRow();
                    value.setBucketKey(row.get("bucket_key"));
                    Number count = row.get("count", Number.class);
                    value.setCount(count == null ? 0 : count.longValue());
                    return value;
                })
                .all();
    }

    @Override
    public Flux<DailyGrowthRow> dailyGrowth(Long tenantId, String table, LocalDateTime from, LocalDateTime to) {
        String physical =
                switch (table) {
                    case "dc3_driver" -> DRIVER;
                    case "dc3_device" -> DEVICE;
                    case "dc3_point" -> POINT;
                    case "dc3_profile" -> PROFILE;
                    default -> throw new IllegalArgumentException("unsupported dashboard growth table: " + table);
                };
        return databaseClient
                .sql("SELECT DATE(create_time) AS day, COUNT(*) AS count FROM " + physical
                        + " WHERE deleted=0 AND tenant_id=:tenant_id AND create_time>=:from_time AND create_time<:to_time"
                        + " GROUP BY DATE(create_time) ORDER BY day ASC")
                .bind("tenant_id", tenantId)
                .bind("from_time", from)
                .bind("to_time", to)
                .map((row, metadata) -> {
                    DailyGrowthRow value = new DailyGrowthRow();
                    Object day = row.get("day");
                    value.setDay(
                            day instanceof LocalDate d
                                    ? d
                                    : day instanceof LocalDateTime d
                                            ? d.toLocalDate()
                                            : LocalDate.parse(day.toString()));
                    Number count = row.get("count", Number.class);
                    value.setCount(count == null ? 0 : count.longValue());
                    return value;
                })
                .all();
    }

    @Override
    public Flux<TopologyDriverRow> topologyDrivers(Long tenantId) {
        return databaseClient
                .sql("SELECT d.id,d.driver_name,COUNT(dev.id) AS device_count FROM " + DRIVER + " d LEFT JOIN " + DEVICE
                        + " dev"
                        + " ON dev.driver_id=d.id AND dev.tenant_id=d.tenant_id AND dev.deleted=0 WHERE d.deleted=0 AND d.tenant_id=:tenant_id"
                        + " GROUP BY d.id,d.driver_name")
                .bind("tenant_id", tenantId)
                .map((row, metadata) -> {
                    TopologyDriverRow value = new TopologyDriverRow();
                    value.setId(longValue(row.get("id")));
                    value.setDriverName(text(row.get("driver_name")));
                    value.setDeviceCount(longValue(row.get("device_count")));
                    return value;
                })
                .all();
    }

    @Override
    public Flux<TopologyDeviceRow> topologyDevicesByDrivers(Long tenantId, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Flux.empty();
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(
                        "SELECT id,device_name,driver_id,CASE WHEN profile_id IS NOT NULL THEN 1 ELSE 0 END AS profile_count FROM "
                                + DEVICE + " WHERE deleted=0 AND tenant_id=:tenant_id AND driver_id IN ("
                                + placeholders(ids.size(), "driver") + ")")
                .bind("tenant_id", tenantId);
        return bindIds(spec, ids, "driver")
                .map((row, metadata) -> {
                    TopologyDeviceRow value = new TopologyDeviceRow();
                    value.setId(longValue(row.get("id")));
                    value.setDeviceName(text(row.get("device_name")));
                    value.setDriverId(longValue(row.get("driver_id")));
                    value.setProfileCount(longValue(row.get("profile_count")));
                    return value;
                })
                .all();
    }

    @Override
    public Flux<ProfileBindingRow> topologyProfileBindings(Long tenantId, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Flux.empty();
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("SELECT profile_id, id AS device_id FROM " + DEVICE
                        + " WHERE deleted=0 AND tenant_id=:tenant_id AND profile_id IS NOT NULL AND id IN ("
                        + placeholders(ids.size(), "device") + ")")
                .bind("tenant_id", tenantId);
        return bindIds(spec, ids, "device")
                .map((row, metadata) -> {
                    ProfileBindingRow value = new ProfileBindingRow();
                    value.setProfileId(longValue(row.get("profile_id")));
                    value.setDeviceId(longValue(row.get("device_id")));
                    return value;
                })
                .all();
    }

    @Override
    public Flux<TopologyProfileRow> topologyProfilesByIds(Long tenantId, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Flux.empty();
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("SELECT id,profile_name FROM " + PROFILE + " WHERE deleted=0 AND tenant_id=:tenant_id AND id IN ("
                        + placeholders(ids.size(), "profile") + ")")
                .bind("tenant_id", tenantId);
        return bindIds(spec, ids, "profile")
                .map((row, metadata) -> {
                    TopologyProfileRow value = new TopologyProfileRow();
                    value.setId(longValue(row.get("id")));
                    value.setProfileName(text(row.get("profile_name")));
                    return value;
                })
                .all();
    }

    @Override
    public Flux<TopologyPointRow> topologyPointsByProfiles(Long tenantId, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Flux.empty();
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("SELECT id,point_name,profile_id FROM " + POINT
                        + " WHERE deleted=0 AND tenant_id=:tenant_id AND profile_id IN ("
                        + placeholders(ids.size(), "profile") + ")")
                .bind("tenant_id", tenantId);
        return bindIds(spec, ids, "profile")
                .map((row, metadata) -> {
                    TopologyPointRow value = new TopologyPointRow();
                    value.setId(longValue(row.get("id")));
                    value.setPointName(text(row.get("point_name")));
                    value.setProfileId(longValue(row.get("profile_id")));
                    return value;
                })
                .all();
    }

    private static String placeholders(int size, String prefix) {
        return IntStream.range(0, size)
                .mapToObj(i -> ":" + prefix + i)
                .reduce((a, b) -> a + "," + b)
                .orElseThrow();
    }

    private static DatabaseClient.GenericExecuteSpec bindIds(
            DatabaseClient.GenericExecuteSpec spec, Collection<Long> ids, String prefix) {
        int i = 0;
        for (Long id : ids) spec = spec.bind(prefix + i++, id);
        return spec;
    }

    private static long longValue(Object value) {
        return value instanceof Number n ? n.longValue() : value == null ? 0 : Long.parseLong(value.toString());
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
