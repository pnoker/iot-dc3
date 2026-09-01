/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.entity.ext.DeviceExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Explicit SQL adapter for manager devices. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, PageTransaction.class})
@RequiredArgsConstructor
public class R2dbcDeviceStore implements ReactiveDeviceStore {

    private static final String TABLE = "dc3_manager.dc3_device";
    private static final String COLUMNS = "id, device_name, device_code, driver_id, profile_id, device_ext, enable_flag,"
            + " tenant_id, remark, signature, version, creator_id, creator_name, create_time, operator_id,"
            + " operator_name, operate_time";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final ObjectMapper objectMapper;
    private final R2dbcDialect dialect;

    @Override
    public Mono<OffsetPage<DeviceBO>> list(DeviceFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("filter is required"));
        String from = TABLE + " d";
        String where = " WHERE d.tenant_id = :tenant_id AND d.deleted = 0";
        List<String> optional = new ArrayList<>();
        if (filter.deviceName() != null && !filter.deviceName().isBlank()) optional.add(" AND d.device_name LIKE :device_name");
        if (filter.deviceCode() != null && !filter.deviceCode().isBlank()) optional.add(" AND d.device_code = :device_code");
        if (filter.driverId() != null) optional.add(" AND d.driver_id = :driver_id");
        if (filter.profileId() != null) optional.add(" AND d.profile_id = :profile_id");
        if (filter.enableFlag() != null) optional.add(" AND d.enable_flag = :enable_flag");
        if (filter.version() != null) optional.add(" AND d.version = :version");
        if (filter.groupId() != null) optional.add(" AND EXISTS (SELECT 1 FROM dc3_manager.dc3_group_bind dgb"
                + " WHERE dgb.deleted = 0 AND dgb.tenant_id = d.tenant_id AND dgb.entity_type_flag = :entity_type"
                + " AND dgb.entity_id = d.id AND dgb.group_id = :group_id)");
        if (filter.labelId() != null) optional.add(" AND EXISTS (SELECT 1 FROM dc3_manager.dc3_label_bind dlb"
                + " WHERE dlb.deleted = 0 AND dlb.tenant_id = d.tenant_id AND dlb.entity_type_flag = :entity_type"
                + " AND dlb.entity_id = d.id AND dlb.label_id = :label_id)");
        String predicates = where + String.join("", optional);
        DatabaseClient.GenericExecuteSpec count = databaseClient.sql("SELECT COUNT(*) AS total FROM " + from + predicates)
                .bind("tenant_id", filter.tenantId());
        DatabaseClient.GenericExecuteSpec rows = databaseClient.sql("SELECT " + COLUMNS + " FROM " + from + predicates
                        + " ORDER BY " + orderBy(filter.sort()) + " LIMIT :limit OFFSET :offset")
                .bind("tenant_id", filter.tenantId())
                .bind("limit", filter.limit())
                .bind("offset", filter.offset());
        count = bindOptional(count, filter);
        rows = bindOptional(rows, filter);
        Mono<Long> total = count.map((row, metadata) -> {
            Number value = row.get("total", Number.class);
            return value == null ? 0L : value.longValue();
        }).one().defaultIfEmpty(0L);
        DatabaseClient.GenericExecuteSpec itemRows = rows;
        return total.flatMap(totalCount -> itemRows.map(this::map).all().collectList()
                        .map(items -> OffsetPage.of(items, filter.offset(), filter.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<DeviceBO> get(Long tenantId, Long id) {
        return one(" WHERE tenant_id = :tenant_id AND id = :id AND deleted = 0", tenantId, id);
    }

    @Override
    public Mono<DeviceBO> getByName(Long tenantId, String deviceName) {
        if (tenantId == null || deviceName == null || deviceName.isBlank()) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id = :tenant_id AND device_name = :device_name AND deleted = 0"
                        + " ORDER BY id DESC LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("device_name", deviceName)
                .map(this::map).one();
    }

    @Override
    public Mono<DeviceBO> getByCode(Long tenantId, String deviceCode) {
        if (tenantId == null || deviceCode == null || deviceCode.isBlank()) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id = :tenant_id AND device_code = :device_code AND deleted = 0"
                        + " ORDER BY id DESC LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("device_code", deviceCode)
                .map(this::map).one();
    }

    @Override
    public Flux<DeviceBO> listByDriverId(Long tenantId, Long driverId) {
        return listSimple(" AND driver_id = :driver_id ORDER BY id DESC", tenantId, "driver_id", driverId);
    }

    @Override
    public Flux<DeviceBO> listByProfileId(Long tenantId, Long profileId) {
        return listSimple(" AND profile_id = :profile_id ORDER BY id DESC", tenantId, "profile_id", profileId);
    }

    @Override
    public Flux<DeviceBO> listByIds(Long tenantId, List<Long> ids) {
        if (tenantId == null || ids == null || ids.isEmpty()) return Flux.empty();
        List<Long> values = ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (values.isEmpty()) return Flux.empty();
        String placeholders = java.util.stream.IntStream.range(0, values.size())
                .mapToObj(index -> ":id" + index).collect(java.util.stream.Collectors.joining(", "));
        DatabaseClient.GenericExecuteSpec query = databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id = :tenant_id AND deleted = 0 AND id IN (" + placeholders + ")"
                        + " ORDER BY id DESC").bind("tenant_id", tenantId);
        for (int index = 0; index < values.size(); index++) query = query.bind("id" + index, values.get(index));
        return query.map(this::map).all();
    }

    @Override
    public Mono<DeviceBO> insert(DeviceBO device) {
        DeviceBO value = prepare(device, device.getId() == null
                ? UuidV7.nextLong() : device.getId(), false);
        DatabaseClient.GenericExecuteSpec insert = databaseClient.sql("INSERT INTO " + TABLE
                        + " (id, device_name, device_code, driver_id, profile_id, device_ext, enable_flag, tenant_id,"
                        + " remark, signature, version, creator_id, creator_name, create_time, operator_id, operator_name,"
                        + " operate_time, deleted) VALUES (:id, :device_name, :device_code, :driver_id, :profile_id,"
                        + " " + dialect.jsonWriteExpression(":device_ext") + ", :enable_flag, :tenant_id, :remark, :signature, :version, :creator_id,"
                        + " :creator_name, :create_time, :operator_id, :operator_name, :operate_time, 0)")
                .bind("id", value.getId()).bind("device_name", value.getDeviceName())
                .bind("device_code", value.getDeviceCode()).bind("driver_id", value.getDriverId())
                .bind("device_ext", serialize(value.getDeviceExt()))
                .bind("enable_flag", enableFlag(value)).bind("tenant_id", value.getTenantId())
                .bind("remark", value.getRemark()).bind("signature", value.getSignature())
                .bind("version", value.getVersion())
                .bind("creator_name", value.getCreatorName()).bind("create_time", utc(value.getCreateTime()))
                .bind("operator_name", value.getOperatorName()).bind("operate_time", utc(value.getOperateTime()))
                ;
        insert = bindNull(insert, "profile_id", value.getProfileId(), Long.class);
        insert = insert.bind("creator_id", value.getCreatorId() == null ? 0L : value.getCreatorId());
        insert = insert.bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId());
        Mono<DeviceBO> write = insert.fetch().rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.error(
                        new IllegalStateException("device insert affected " + rows + " rows")));
        return transactionalOperator.transactional(validateRelations(value).then(write));
    }

    @Override
    public Mono<DeviceBO> update(DeviceBO device, int expectedVersion) {
        DeviceBO value = prepare(device, device.getId(), true);
        DatabaseClient.GenericExecuteSpec update = databaseClient.sql("UPDATE " + TABLE + " SET device_name = :device_name, driver_id = :driver_id,"
                        + " profile_id = :profile_id, device_ext = " + dialect.jsonWriteExpression(":device_ext") + ", enable_flag = :enable_flag,"
                        + " remark = :remark, signature = :signature, version = version + 1, operator_id = :operator_id,"
                        + " operator_name = :operator_name, operate_time = :operate_time WHERE id = :id"
                        + " AND tenant_id = :tenant_id AND version = :expected_version AND deleted = 0")
                .bind("device_name", value.getDeviceName()).bind("driver_id", value.getDriverId())
                .bind("device_ext", serialize(value.getDeviceExt()))
                .bind("enable_flag", enableFlag(value)).bind("remark", value.getRemark())
                .bind("signature", value.getSignature()).bind("operator_name", value.getOperatorName())
                .bind("operate_time", utcNow())
                .bind("id", value.getId()).bind("tenant_id", value.getTenantId())
                .bind("expected_version", expectedVersion);
        update = bindNull(update, "profile_id", value.getProfileId(), Long.class);
        update = update.bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId());
        Mono<DeviceBO> write = update.fetch().rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty());
        return transactionalOperator.transactional(validateRelations(value).then(write));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null) return Mono.just(false);
        DatabaseClient.GenericExecuteSpec write = databaseClient.sql("UPDATE " + TABLE + " SET deleted = 1, operator_id = :operator_id,"
                        + " operator_name = :operator_name, operate_time = :operate_time"
                        + " WHERE id = :id AND tenant_id = :tenant_id AND version = :expected_version AND deleted = 0")
                .bind("operator_name", operatorName == null ? "" : operatorName)
                .bind("operate_time", utcNow()).bind("id", id).bind("tenant_id", tenantId)
                .bind("expected_version", expectedVersion);
        write = write.bind("operator_id", operatorId == null ? 0L : operatorId);
        return transactionalOperator.transactional(write.fetch().rowsUpdated().map(rows -> rows == 1));
    }

    private DatabaseClient.GenericExecuteSpec bindOptional(DatabaseClient.GenericExecuteSpec spec, DeviceFilter filter) {
        if (filter.deviceName() != null && !filter.deviceName().isBlank()) spec = spec.bind("device_name", "%" + filter.deviceName().trim() + "%");
        if (filter.deviceCode() != null && !filter.deviceCode().isBlank()) spec = spec.bind("device_code", filter.deviceCode().trim());
        if (filter.driverId() != null) spec = spec.bind("driver_id", filter.driverId());
        if (filter.profileId() != null) spec = spec.bind("profile_id", filter.profileId());
        if (filter.enableFlag() != null) spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        if (filter.version() != null) spec = spec.bind("version", filter.version());
        if (filter.groupId() != null) spec = spec.bind("group_id", filter.groupId());
        if (filter.labelId() != null) spec = spec.bind("label_id", filter.labelId());
        if (filter.groupId() != null || filter.labelId() != null) {
            spec = spec.bind("entity_type", EntityTypeEnum.DEVICE.getIndex());
        }
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec bindNull(DatabaseClient.GenericExecuteSpec spec, String name,
                                                        Object value, Class<?> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    private Mono<Void> validateRelations(DeviceBO value) {
        Mono<Boolean> driver = databaseClient.sql("SELECT 1 FROM dc3_manager.dc3_driver"
                        + " WHERE id = :driver_id AND tenant_id = :tenant_id AND deleted = 0 LIMIT 1")
                .bind("driver_id", value.getDriverId()).bind("tenant_id", value.getTenantId())
                .map((row, metadata) -> true).one().defaultIfEmpty(false);
        Mono<Boolean> profile = value.getProfileId() == null
                ? Mono.just(true)
                : databaseClient.sql("SELECT 1 FROM dc3_manager.dc3_profile"
                                + " WHERE id = :profile_id AND tenant_id = :tenant_id AND deleted = 0 LIMIT 1")
                        .bind("profile_id", value.getProfileId()).bind("tenant_id", value.getTenantId())
                        .map((row, metadata) -> true).one().defaultIfEmpty(false);
        return Mono.zip(driver, profile).flatMap(tuple -> tuple.getT1() && tuple.getT2()
                ? Mono.empty()
                : Mono.error(new RequestException("Device relations must belong to the same tenant")));
    }

    private String orderBy(List<SortSpec> sort) {
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column = switch (spec.field()) {
                case "deviceName" -> "d.device_name";
                case "deviceCode" -> "d.device_code";
                case "version" -> "d.version";
                case "createTime" -> "d.create_time";
                case "operateTime" -> "d.operate_time";
                case "id" -> "d.id";
                default -> throw new IllegalArgumentException("device sort field is not allowed: " + spec.field());
            };
            clauses.add(column + (spec.direction() == SortSpec.Direction.ASC ? " ASC" : " DESC"));
        }
        if (clauses.stream().noneMatch(clause -> clause.startsWith("d.id "))) clauses.add("d.id DESC");
        return String.join(", ", clauses);
    }

    private Mono<DeviceBO> one(String predicate, Long tenantId, Long id) {
        if (tenantId == null || id == null) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + predicate + " LIMIT 1")
                .bind("tenant_id", tenantId).bind("id", id).map(this::map).one();
    }

    private Flux<DeviceBO> listSimple(String suffix, Long tenantId, String key, Long value) {
        if (tenantId == null || value == null) return Flux.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id = :tenant_id AND deleted = 0" + suffix)
                .bind("tenant_id", tenantId).bind(key, value).map(this::map).all();
    }

    private DeviceBO prepare(DeviceBO source, Long id, boolean update) {
        if (source == null || source.getTenantId() == null || source.getDeviceName() == null
                || source.getDeviceCode() == null || source.getDriverId() == null) {
            throw new IllegalArgumentException("device identity fields are required");
        }
        source.setId(id);
        source.setDeviceName(source.getDeviceName().trim());
        source.setDeviceCode(source.getDeviceCode().trim());
        source.setRemark(source.getRemark() == null ? "" : source.getRemark());
        source.setSignature(source.getSignature() == null ? "" : source.getSignature());
        source.setVersion(source.getVersion() == null ? 0 : source.getVersion());
        source.setCreatorId(source.getCreatorId() == null
                ? (source.getOperatorId() == null ? 0L : source.getOperatorId()) : source.getCreatorId());
        source.setCreatorName(source.getCreatorName() == null
                ? (source.getOperatorName() == null ? "" : source.getOperatorName()) : source.getCreatorName());
        source.setOperatorId(source.getOperatorId() == null ? 0L : source.getOperatorId());
        source.setOperatorName(source.getOperatorName() == null ? "" : source.getOperatorName());
        source.setCreateTime(source.getCreateTime() == null ? utcNow() : source.getCreateTime());
        source.setOperateTime(utcNow());
        return source;
    }

    private DeviceBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        DeviceBO value = new DeviceBO();
        value.setId(row.get("id", Long.class));
        value.setDeviceName(row.get("device_name", String.class));
        value.setDeviceCode(row.get("device_code", String.class));
        value.setDriverId(row.get("driver_id", Long.class));
        value.setProfileId(row.get("profile_id", Long.class));
        value.setDeviceExt(deserialize(row.get("device_ext", String.class)));
        Number enabled = row.get("enable_flag", Number.class);
        value.setEnableFlag(EnableFlagEnum.ofIndex(enabled == null ? null : enabled.byteValue()));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setRemark(row.get("remark", String.class));
        value.setSignature(row.get("signature", String.class));
        value.setVersion(row.get("version", Integer.class));
        value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class));
        value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(time(row.get("operate_time")));
        return value;
    }

    private Byte enableFlag(DeviceBO value) {
        return value.getEnableFlag() == null ? EnableFlagEnum.ENABLE.getIndex() : value.getEnableFlag().getIndex();
    }

    private String serialize(DeviceExt value) {
        try {
            return value == null ? "{}" : objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("device_ext is not valid JSON", exception);
        }
    }

    private DeviceExt deserialize(String value) {
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, DeviceExt.class);
        } catch (Exception exception) {
            throw new IllegalStateException("device_ext contains invalid JSON", exception);
        }
    }

    private LocalDateTime utc(LocalDateTime value) {
        return value == null ? utcNow() : value;
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset) return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return null;
    }
}
