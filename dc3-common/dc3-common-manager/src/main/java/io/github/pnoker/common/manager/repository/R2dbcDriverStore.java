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
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.entity.ext.DriverExt;
import io.github.pnoker.common.enums.DriverTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/** Explicit SQL adapter for manager drivers. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcDriverStore implements ReactiveDriverStore {

    private static final String TABLE = "dc3_manager.dc3_driver";
    private static final String COLUMNS = "id, driver_name, driver_code, service_name, service_host, driver_type_flag,"
            + " driver_ext, enable_flag, tenant_id, remark, signature, version, creator_id, creator_name, create_time,"
            + " operator_id, operator_name, operate_time";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final ObjectMapper objectMapper;
    private final R2dbcDialect dialect;

    @Override
    public Mono<OffsetPage<DriverBO>> list(DriverFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("filter is required"));
        StringBuilder where = new StringBuilder(" WHERE d.tenant_id = :tenant_id AND d.deleted = 0");
        if (present(filter.driverName())) where.append(" AND d.driver_name LIKE :driver_name");
        if (present(filter.driverCode())) where.append(" AND d.driver_code = :driver_code");
        if (present(filter.serviceName())) where.append(" AND d.service_name = :service_name");
        if (present(filter.serviceHost())) where.append(" AND d.service_host = :service_host");
        if (filter.driverTypeFlag() != null) where.append(" AND d.driver_type_flag = :driver_type_flag");
        if (filter.enableFlag() != null) where.append(" AND d.enable_flag = :enable_flag");
        if (filter.version() != null) where.append(" AND d.version = :version");
        if (filter.groupId() != null)
            where.append(" AND EXISTS (SELECT 1 FROM dc3_manager.dc3_group_bind dgb"
                    + " WHERE dgb.deleted = 0 AND dgb.tenant_id = d.tenant_id AND dgb.entity_type_flag = :entity_type"
                    + " AND dgb.entity_id = d.id AND dgb.group_id = :group_id)");
        if (filter.labelId() != null)
            where.append(" AND EXISTS (SELECT 1 FROM dc3_manager.dc3_label_bind dlb"
                    + " WHERE dlb.deleted = 0 AND dlb.tenant_id = d.tenant_id AND dlb.entity_type_flag = :entity_type"
                    + " AND dlb.entity_id = d.id AND dlb.label_id = :label_id)");

        String order = orderBy(filter.sort());
        DatabaseClient.GenericExecuteSpec countSpec = databaseClient
                .sql("SELECT COUNT(*) AS total FROM " + TABLE + " d" + where)
                .bind("tenant_id", filter.tenantId());
        DatabaseClient.GenericExecuteSpec itemSpec = databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " d" + where + order + " LIMIT :limit OFFSET :offset")
                .bind("tenant_id", filter.tenantId())
                .bind("limit", filter.limit())
                .bind("offset", filter.offset());
        countSpec = bindFilter(countSpec, filter);
        itemSpec = bindFilter(itemSpec, filter);
        Mono<Long> total = countSpec
                .map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                })
                .one();
        Mono<List<DriverBO>> items = itemSpec.map(this::map).all().collectList();
        return total.flatMap(totalCount ->
                        items.map(pageItems -> OffsetPage.of(pageItems, filter.offset(), filter.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<DriverBO> get(Long tenantId, Long id) {
        if (tenantId == null || id == null) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id = :tenant_id AND id = :id AND deleted = 0")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<DriverBO> getByNameAndCode(Long tenantId, String driverName, String driverCode) {
        if (tenantId == null || !present(driverName) || !present(driverCode)) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id = :tenant_id AND driver_name = :driver_name AND driver_code = :driver_code"
                        + " AND deleted = 0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("driver_name", driverName)
                .bind("driver_code", driverCode)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<DriverBO> getByServiceName(Long tenantId, String serviceName) {
        if (tenantId == null || !present(serviceName)) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id = :tenant_id AND service_name = :service_name AND deleted = 0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("service_name", serviceName)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<DriverBO> getByDeviceId(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Mono.empty();
        return databaseClient
                .sql("SELECT " + qualifiedColumns("d") + " FROM " + TABLE + " d"
                        + " JOIN dc3_manager.dc3_device dv ON dv.driver_id = d.id"
                        + " WHERE d.tenant_id = :tenant_id AND dv.tenant_id = :tenant_id AND dv.id = :device_id"
                        + " AND d.deleted = 0 AND dv.deleted = 0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("device_id", deviceId)
                .map(this::map)
                .one();
    }

    @Override
    public Flux<DriverBO> listByIds(Long tenantId, List<Long> ids) {
        if (tenantId == null || ids == null || ids.isEmpty()) return Flux.empty();
        List<Long> values =
                ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (values.isEmpty()) return Flux.empty();
        String placeholders = java.util.stream.IntStream.range(0, values.size())
                .mapToObj(i -> ":id" + i)
                .collect(java.util.stream.Collectors.joining(","));
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id = :tenant_id AND deleted = 0 AND id IN (" + placeholders + ") ORDER BY id")
                .bind("tenant_id", tenantId);
        for (int i = 0; i < values.size(); i++) spec = spec.bind("id" + i, values.get(i));
        return spec.map(this::map).all();
    }

    @Override
    public Flux<DriverBO> listByProfileId(Long tenantId, Long profileId) {
        if (tenantId == null || profileId == null) return Flux.empty();
        return databaseClient
                .sql("SELECT DISTINCT " + qualifiedColumns("d")
                        + " FROM " + TABLE + " d JOIN dc3_manager.dc3_device dv ON dv.driver_id = d.id"
                        + " WHERE d.tenant_id = :tenant_id AND dv.tenant_id = :tenant_id AND dv.profile_id = :profile_id"
                        + " AND d.deleted = 0 AND dv.deleted = 0 ORDER BY d.id")
                .bind("tenant_id", tenantId)
                .bind("profile_id", profileId)
                .map(this::map)
                .all();
    }

    @Override
    public Flux<DriverBO> listByPointId(Long tenantId, Long pointId) {
        if (tenantId == null || pointId == null) return Flux.empty();
        return databaseClient
                .sql("SELECT DISTINCT " + qualifiedColumns("d")
                        + " FROM " + TABLE + " d"
                        + " JOIN dc3_manager.dc3_device dv ON dv.tenant_id = d.tenant_id AND dv.driver_id = d.id"
                        + " JOIN dc3_manager.dc3_point p ON p.tenant_id = dv.tenant_id AND p.profile_id = dv.profile_id"
                        + " WHERE d.tenant_id = :tenant_id AND p.id = :point_id"
                        + " AND d.deleted = 0 AND dv.deleted = 0 AND p.deleted = 0 ORDER BY d.id")
                .bind("tenant_id", tenantId)
                .bind("point_id", pointId)
                .map(this::map)
                .all();
    }

    @Override
    public Mono<DriverBO> insert(DriverBO value) {
        if (value.getId() == null) value.setId(UuidV7.nextLong());
        if (!present(value.getDriverCode())) value.setDriverCode(CodeUtil.getCode());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("INSERT INTO " + TABLE + " (" + COLUMNS
                        + ", deleted) VALUES (:id, :driver_name, :driver_code, :service_name, :service_host, :driver_type_flag,"
                        + " " + dialect.jsonWriteExpression(":driver_ext")
                        + ", :enable_flag, :tenant_id, :remark, :signature, :version, :creator_id,"
                        + " :creator_name, :create_time, :operator_id, :operator_name, :operate_time, 0)")
                .bind("id", value.getId())
                .bind("driver_name", valueOrEmpty(value.getDriverName()))
                .bind("driver_code", valueOrEmpty(value.getDriverCode()))
                .bind("service_name", valueOrEmpty(value.getServiceName()))
                .bind("service_host", valueOrEmpty(value.getServiceHost()))
                .bind("driver_type_flag", enumIndex(value.getDriverTypeFlag()))
                .bind("driver_ext", serialize(value.getDriverExt()))
                .bind("enable_flag", enumIndex(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("remark", valueOrEmpty(value.getRemark()))
                .bind("signature", valueOrEmpty(value.getSignature()))
                .bind("version", value.getVersion() == null ? 0 : value.getVersion())
                .bind("creator_name", valueOrEmpty(value.getCreatorName()))
                .bind("create_time", now)
                .bind("operator_name", valueOrEmpty(value.getOperatorName()))
                .bind("operate_time", now);
        spec = spec.bind("creator_id", value.getCreatorId() == null ? 0L : value.getCreatorId());
        spec = spec.bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId());
        return transactionalOperator.transactional(
                spec.fetch().rowsUpdated().then(get(value.getTenantId(), value.getId())));
    }

    @Override
    public Mono<DriverBO> update(DriverBO value, int expectedVersion) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("UPDATE " + TABLE + " SET driver_name = :driver_name,"
                        + " driver_code = :driver_code, service_name = :service_name, service_host = :service_host,"
                        + " driver_type_flag = :driver_type_flag, driver_ext = "
                        + dialect.jsonWriteExpression(":driver_ext") + ", enable_flag = :enable_flag,"
                        + " remark = :remark, signature = :signature, version = version + 1, operator_id = :operator_id,"
                        + " operator_name = :operator_name, operate_time = :operate_time WHERE id = :id AND tenant_id = :tenant_id"
                        + " AND version = :expected_version AND deleted = 0")
                .bind("driver_name", valueOrEmpty(value.getDriverName()))
                .bind("driver_code", valueOrEmpty(value.getDriverCode()))
                .bind("service_name", valueOrEmpty(value.getServiceName()))
                .bind("service_host", valueOrEmpty(value.getServiceHost()))
                .bind("driver_type_flag", enumIndex(value.getDriverTypeFlag()))
                .bind("driver_ext", serialize(value.getDriverExt()))
                .bind("enable_flag", enumIndex(value.getEnableFlag()))
                .bind("remark", valueOrEmpty(value.getRemark()))
                .bind("signature", valueOrEmpty(value.getSignature()))
                .bind("operator_name", valueOrEmpty(value.getOperatorName()))
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .bind("id", value.getId())
                .bind("tenant_id", value.getTenantId())
                .bind("expected_version", expectedVersion);
        spec = spec.bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId());
        return transactionalOperator.transactional(spec.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("UPDATE " + TABLE + " SET deleted = 1, operator_id = :operator_id,"
                        + " operator_name = :operator_name, operate_time = :operate_time WHERE tenant_id = :tenant_id AND id = :id"
                        + " AND version = :expected_version AND deleted = 0")
                .bind("operator_name", valueOrEmpty(operatorName))
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .bind("expected_version", expectedVersion);
        spec = spec.bind("operator_id", operatorId == null ? 0L : operatorId);
        return transactionalOperator.transactional(spec.fetch().rowsUpdated().map(rows -> rows == 1));
    }

    private DatabaseClient.GenericExecuteSpec bindFilter(DatabaseClient.GenericExecuteSpec spec, DriverFilter filter) {
        if (present(filter.driverName()))
            spec = spec.bind("driver_name", "%" + filter.driverName().trim() + "%");
        if (present(filter.driverCode()))
            spec = spec.bind("driver_code", filter.driverCode().trim());
        if (present(filter.serviceName()))
            spec = spec.bind("service_name", filter.serviceName().trim());
        if (present(filter.serviceHost()))
            spec = spec.bind("service_host", filter.serviceHost().trim());
        if (filter.driverTypeFlag() != null)
            spec = spec.bind("driver_type_flag", filter.driverTypeFlag().getIndex());
        if (filter.enableFlag() != null)
            spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        if (filter.version() != null) spec = spec.bind("version", filter.version());
        if (filter.groupId() != null) spec = spec.bind("group_id", filter.groupId());
        if (filter.labelId() != null) spec = spec.bind("label_id", filter.labelId());
        if (filter.groupId() != null || filter.labelId() != null)
            spec = spec.bind("entity_type", EntityTypeEnum.DRIVER.getIndex());
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        List<String> values = new ArrayList<>();
        if (sort != null)
            for (SortSpec spec : sort) {
                String column =
                        switch (spec.field()) {
                            case "id" -> "d.id";
                            case "driverName" -> "d.driver_name";
                            case "driverCode" -> "d.driver_code";
                            case "serviceName" -> "d.service_name";
                            case "serviceHost" -> "d.service_host";
                            case "createTime" -> "d.create_time";
                            case "operateTime" -> "d.operate_time";
                            case "version" -> "d.version";
                            default ->
                                throw new IllegalArgumentException("unsupported driver sort field: " + spec.field());
                        };
                values.add(column + (spec.direction() == SortSpec.Direction.DESC ? " DESC" : " ASC"));
            }
        return " ORDER BY " + (values.isEmpty() ? "d.id ASC" : String.join(", ", values) + ", d.id ASC");
    }

    private DriverBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        DriverBO value = new DriverBO();
        value.setId(row.get("id", Long.class));
        value.setDriverName(row.get("driver_name", String.class));
        value.setDriverCode(row.get("driver_code", String.class));
        value.setServiceName(row.get("service_name", String.class));
        value.setServiceHost(row.get("service_host", String.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setRemark(row.get("remark", String.class));
        value.setSignature(row.get("signature", String.class));
        value.setVersion(row.get("version", Integer.class));
        value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(toLocalDateTime(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class));
        value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(toLocalDateTime(row.get("operate_time")));
        Number type = row.get("driver_type_flag", Number.class);
        value.setDriverTypeFlag(DriverTypeEnum.ofIndex(type == null ? null : type.byteValue()));
        Number enabled = row.get("enable_flag", Number.class);
        value.setEnableFlag(EnableFlagEnum.ofIndex(enabled == null ? null : enabled.byteValue()));
        String ext = row.get("driver_ext", String.class);
        value.setDriverExt(deserialize(ext == null ? null : ext));
        return value;
    }

    private String qualifiedColumns(String alias) {
        return java.util.Arrays.stream(COLUMNS.split(", "))
                .map(column -> alias + "." + column)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private DriverExt deserialize(String json) {
        try {
            return json == null ? null : objectMapper.readValue(json, DriverExt.class);
        } catch (Exception e) {
            throw new IllegalStateException("invalid driver_ext JSON", e);
        }
    }

    private String serialize(DriverExt ext) {
        try {
            return objectMapper.writeValueAsString(ext == null ? new DriverExt() : ext);
        } catch (Exception e) {
            throw new IllegalArgumentException("driver_ext is not valid JSON", e);
        }
    }

    private Byte enumIndex(DriverTypeEnum value) {
        return value == null ? DriverTypeEnum.DRIVER_CLIENT.getIndex() : value.getIndex();
    }

    private Byte enumIndex(EnableFlagEnum value) {
        return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof java.time.OffsetDateTime offsetDateTime) return offsetDateTime.toLocalDateTime();
        if (value instanceof java.time.Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return null;
    }
}
