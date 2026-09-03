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

import io.github.pnoker.common.manager.repository.PointFilter;
import io.github.pnoker.common.manager.repository.ReactivePointStore;

import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.PointExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.math.BigDecimal;
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

/** Explicit SQL adapter for manager points. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcPointStore implements ReactivePointStore {
    private static final String TABLE = "dc3_manager.dc3_point";
    private static final String COLUMNS =
            "id, point_name, point_code, point_type_flag, rw_flag, base_value, multiple, value_decimal, unit, profile_id, point_ext, enable_flag, tenant_id, remark, signature, version, creator_id, creator_name, create_time, operator_id, operator_name, operate_time";
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final ObjectMapper objectMapper;
    private final R2dbcDialect dialect;

    @Override
    public Mono<Boolean> existsByNameOrCode(
            Long tenantId, Long profileId, String pointName, String pointCode, Long excludingId) {
        if (tenantId == null || profileId == null || (pointName == null && pointCode == null)) return Mono.just(false);
        List<String> predicates = new ArrayList<>();
        if (pointName != null && !pointName.isBlank()) predicates.add("point_name=:point_name");
        if (pointCode != null && !pointCode.isBlank()) predicates.add("point_code=:point_code");
        if (predicates.isEmpty()) return Mono.just(false);
        String sql =
                "SELECT 1 FROM " + TABLE + " WHERE tenant_id=:tenant_id AND profile_id=:profile_id AND deleted=0 AND ("
                        + String.join(" OR ", predicates) + ")" + (excludingId == null ? "" : " AND id<>:excluding_id")
                        + " LIMIT 1";
        DatabaseClient.GenericExecuteSpec query =
                databaseClient.sql(sql).bind("tenant_id", tenantId).bind("profile_id", profileId);
        if (pointName != null && !pointName.isBlank()) query = query.bind("point_name", pointName);
        if (pointCode != null && !pointCode.isBlank()) query = query.bind("point_code", pointCode);
        if (excludingId != null) query = query.bind("excluding_id", excludingId);
        return query.map((row, metadata) -> true).one().defaultIfEmpty(false);
    }

    @Override
    public Mono<PointBO> insert(PointBO value) {
        if (value == null
                || value.getTenantId() == null
                || value.getProfileId() == null
                || value.getPointName() == null
                || value.getPointName().isBlank())
            return Mono.error(new IllegalArgumentException("tenantId, profileId and pointName are required"));
        if (value.getId() == null) value.setId(UuidV7.nextLong());
        if (value.getVersion() == null) value.setVersion(0);
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(
                        "INSERT INTO " + TABLE
                                + " (id,point_name,point_code,point_type_flag,rw_flag,base_value,multiple,value_decimal,unit,profile_id,point_ext,enable_flag,tenant_id,remark,signature,version,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:point_name,:point_code,:point_type,:rw_flag,:base_value,:multiple,:value_decimal,:unit,:profile_id,"
                                + dialect.jsonWriteExpression(":point_ext")
                                + ",:enable_flag,:tenant_id,:remark,:signature,:version,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", value.getId())
                .bind("point_name", value.getPointName().trim())
                .bind("point_code", value.getPointCode() == null ? "" : value.getPointCode())
                .bind("point_type", index(value.getPointTypeFlag()))
                .bind("rw_flag", index(value.getRwFlag()))
                .bind("base_value", value.getBaseValue() == null ? BigDecimal.ZERO : value.getBaseValue())
                .bind("multiple", value.getMultiple() == null ? BigDecimal.ONE : value.getMultiple())
                .bind("value_decimal", value.getValueDecimal() == null ? (byte) 6 : value.getValueDecimal())
                .bind("unit", value.getUnit() == null ? "" : value.getUnit())
                .bind("profile_id", value.getProfileId())
                .bind("point_ext", serialize(value.getPointExt()))
                .bind("enable_flag", index(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("signature", value.getSignature() == null ? "" : value.getSignature())
                .bind("version", value.getVersion())
                .bind("creator_id", value.getCreatorId() == null ? 0L : value.getCreatorId())
                .bind("creator_name", value.getCreatorName() == null ? "" : value.getCreatorName())
                .bind("create_time", value.getCreateTime() == null ? now : value.getCreateTime())
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", value.getOperatorName() == null ? "" : value.getOperatorName())
                .bind("operate_time", value.getOperateTime() == null ? now : value.getOperateTime());
        return transactionalOperator.transactional(query.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? get(value.getTenantId(), value.getId())
                        : Mono.error(new IllegalStateException("point insert affected " + rows + " rows"))));
    }

    @Override
    public Mono<PointBO> update(PointBO value, int expectedVersion) {
        if (value == null || value.getTenantId() == null || value.getId() == null)
            return Mono.error(new IllegalArgumentException("tenantId and point id are required"));
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET point_name=:point_name,point_type_flag=:point_type,rw_flag=:rw_flag,base_value=:base_value,multiple=:multiple,value_decimal=:value_decimal,unit=:unit,profile_id=:profile_id,point_ext="
                                + dialect.jsonWriteExpression(":point_ext")
                                + ",enable_flag=:enable_flag,remark=:remark,signature=:signature,version=version+1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE id=:id AND tenant_id=:tenant_id AND version=:expected_version AND deleted=0")
                .bind("point_name", value.getPointName().trim())
                .bind("point_type", index(value.getPointTypeFlag()))
                .bind("rw_flag", index(value.getRwFlag()))
                .bind("base_value", value.getBaseValue() == null ? BigDecimal.ZERO : value.getBaseValue())
                .bind("multiple", value.getMultiple() == null ? BigDecimal.ONE : value.getMultiple())
                .bind("value_decimal", value.getValueDecimal() == null ? (byte) 6 : value.getValueDecimal())
                .bind("unit", value.getUnit() == null ? "" : value.getUnit())
                .bind("profile_id", value.getProfileId())
                .bind("point_ext", serialize(value.getPointExt()))
                .bind("enable_flag", index(value.getEnableFlag()))
                .bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("signature", value.getSignature() == null ? "" : value.getSignature())
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", value.getOperatorName() == null ? "" : value.getOperatorName())
                .bind("operate_time", java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))
                .bind("id", value.getId())
                .bind("tenant_id", value.getTenantId())
                .bind("expected_version", expectedVersion);
        return transactionalOperator.transactional(query.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND version=:expected_version AND deleted=0")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .bind("expected_version", expectedVersion)
                .bind("operator_id", operatorId == null ? 0L : operatorId)
                .bind("operator_name", operatorName == null ? "" : operatorName)
                .bind("operate_time", java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1));
    }

    @Override
    public Mono<PointBO> get(Long tenantId, Long id) {
        if (tenantId == null || id == null) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<OffsetPage<PointBO>> list(PointFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("filter is required"));
        StringBuilder where = new StringBuilder(" WHERE p.tenant_id=:tenant_id AND p.deleted=0");
        if (filter.pointName() != null && !filter.pointName().isBlank())
            where.append(" AND p.point_name LIKE :point_name");
        if (filter.pointCode() != null && !filter.pointCode().isBlank()) where.append(" AND p.point_code=:point_code");
        if (filter.pointTypeFlag() != null) where.append(" AND p.point_type_flag=:point_type");
        if (filter.rwFlag() != null) where.append(" AND p.rw_flag=:rw_flag");
        if (filter.profileId() != null) where.append(" AND p.profile_id=:profile_id");
        if (filter.enableFlag() != null) where.append(" AND p.enable_flag=:enable_flag");
        if (filter.version() != null) where.append(" AND p.version=:version");
        if (filter.deviceId() != null)
            where.append(
                    " AND EXISTS (SELECT 1 FROM "
                            + "dc3_manager.dc3_device d WHERE d.tenant_id=p.tenant_id AND d.id=:device_id AND d.profile_id=p.profile_id AND d.deleted=0)");
        if (filter.groupId() != null)
            where.append(
                    " AND EXISTS (SELECT 1 FROM dc3_manager.dc3_group_bind dgb WHERE dgb.deleted=0 AND dgb.tenant_id=p.tenant_id AND dgb.entity_id=p.id AND dgb.group_id=:group_id AND dgb.entity_type_flag=:entity_type)");
        if (filter.labelId() != null)
            where.append(
                    " AND EXISTS (SELECT 1 FROM dc3_manager.dc3_label_bind dlb WHERE dlb.deleted=0 AND dlb.tenant_id=p.tenant_id AND dlb.entity_id=p.id AND dlb.label_id=:label_id AND dlb.entity_type_flag=:entity_type)");
        String predicate = where.toString();
        DatabaseClient.GenericExecuteSpec count =
                bind(databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + " p" + predicate), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(
                        databaseClient.sql("SELECT " + qualifiedColumns("p") + " FROM " + TABLE + " p" + predicate
                                + " ORDER BY " + orderBy(filter.sort()) + " LIMIT :limit OFFSET :offset"),
                        filter)
                .bind("limit", filter.limit())
                .bind("offset", filter.offset());
        Mono<Long> total = count.map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                })
                .one()
                .defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(this::map)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(items, filter.offset(), filter.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Flux<PointBO> listByIds(Long tenantId, List<Long> ids) {
        if (tenantId == null || ids == null || ids.isEmpty()) return Flux.empty();
        List<Long> values =
                ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (values.isEmpty()) return Flux.empty();
        String placeholders = java.util.stream.IntStream.range(0, values.size())
                .mapToObj(i -> ":id" + i)
                .reduce((a, b) -> a + "," + b)
                .orElseThrow();
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND deleted=0 AND id IN ("
                        + placeholders + ") ORDER BY id")
                .bind("tenant_id", tenantId);
        for (int i = 0; i < values.size(); i++) spec = spec.bind("id" + i, values.get(i));
        return spec.map(this::map).all();
    }

    @Override
    public Flux<PointBO> listByProfileId(Long tenantId, Long profileId) {
        if (tenantId == null || profileId == null) return Flux.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND profile_id=:profile_id AND deleted=0 ORDER BY id")
                .bind("tenant_id", tenantId)
                .bind("profile_id", profileId)
                .map(this::map)
                .all();
    }

    @Override
    public Flux<PointBO> listByDeviceId(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Flux.empty();
        return databaseClient
                .sql(
                        "SELECT " + qualifiedColumns("p") + " FROM " + TABLE + " p JOIN "
                                + "dc3_manager.dc3_device d ON d.tenant_id=p.tenant_id AND d.profile_id=p.profile_id WHERE d.tenant_id=:tenant_id AND d.id=:device_id AND d.deleted=0 AND p.deleted=0 ORDER BY p.id")
                .bind("tenant_id", tenantId)
                .bind("device_id", deviceId)
                .map(this::map)
                .all();
    }

    @Override
    public Flux<Long> listConfiguredDeviceIdsByPointId(Long tenantId, Long pointId) {
        if (tenantId == null || pointId == null) return Flux.empty();
        return databaseClient
                .sql(
                        "SELECT DISTINCT d.id FROM " + TABLE
                                + " p JOIN dc3_manager.dc3_device d ON d.tenant_id=p.tenant_id AND d.profile_id=p.profile_id AND d.deleted=0 JOIN dc3_manager.dc3_point_attribute_config c ON c.tenant_id=p.tenant_id AND c.device_id=d.id AND c.point_id=p.id AND c.deleted=0 WHERE p.tenant_id=:tenant_id AND p.id=:point_id AND p.deleted=0 ORDER BY d.id")
                .bind("tenant_id", tenantId)
                .bind("point_id", pointId)
                .map((row, metadata) -> row.get("id", Long.class))
                .all();
    }

    @Override
    public Mono<Long> countByDeviceId(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Mono.just(0L);
        return databaseClient
                .sql(
                        "SELECT COUNT(*) AS total FROM " + TABLE
                                + " p JOIN dc3_manager.dc3_device d ON d.tenant_id=p.tenant_id AND d.profile_id=p.profile_id AND d.deleted=0 WHERE d.tenant_id=:tenant_id AND d.id=:device_id AND p.deleted=0")
                .bind("tenant_id", tenantId)
                .bind("device_id", deviceId)
                .map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                })
                .one()
                .defaultIfEmpty(0L);
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, PointFilter filter) {
        spec = spec.bind("tenant_id", filter.tenantId());
        if (filter.pointName() != null && !filter.pointName().isBlank())
            spec = spec.bind("point_name", "%" + filter.pointName() + "%");
        if (filter.pointCode() != null && !filter.pointCode().isBlank())
            spec = spec.bind("point_code", filter.pointCode());
        if (filter.pointTypeFlag() != null)
            spec = spec.bind("point_type", filter.pointTypeFlag().getIndex());
        if (filter.rwFlag() != null) spec = spec.bind("rw_flag", filter.rwFlag().getIndex());
        if (filter.profileId() != null) spec = spec.bind("profile_id", filter.profileId());
        if (filter.enableFlag() != null)
            spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        if (filter.version() != null) spec = spec.bind("version", filter.version());
        if (filter.deviceId() != null) spec = spec.bind("device_id", filter.deviceId());
        if (filter.groupId() != null) spec = spec.bind("group_id", filter.groupId());
        if (filter.labelId() != null) spec = spec.bind("label_id", filter.labelId());
        if (filter.groupId() != null || filter.labelId() != null)
            spec = spec.bind("entity_type", EntityTypeEnum.POINT.getIndex());
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "p.id ASC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String c =
                    switch (spec.field()) {
                        case "pointName" -> "p.point_name";
                        case "pointCode" -> "p.point_code";
                        case "createTime" -> "p.create_time";
                        case "id" -> "p.id";
                        default -> throw new IllegalArgumentException("unsupported sort field: " + spec.field());
                    };
            clauses.add(c + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(c -> c.startsWith("p.id"))) clauses.add("p.id ASC");
        return String.join(", ", clauses);
    }

    private String qualifiedColumns(String alias) {
        return java.util.Arrays.stream(COLUMNS.split(", "))
                .map(c -> alias + "." + c)
                .reduce((a, b) -> a + ", " + b)
                .orElse(COLUMNS);
    }

    private PointBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        PointBO v = new PointBO();
        v.setId(row.get("id", Long.class));
        v.setPointName(row.get("point_name", String.class));
        v.setPointCode(row.get("point_code", String.class));
        v.setTenantId(row.get("tenant_id", Long.class));
        v.setProfileId(row.get("profile_id", Long.class));
        v.setUnit(row.get("unit", String.class));
        Number decimals = row.get("value_decimal", Number.class);
        v.setValueDecimal(decimals == null ? null : decimals.byteValue());
        v.setVersion(row.get("version", Integer.class));
        v.setRemark(row.get("remark", String.class));
        v.setSignature(row.get("signature", String.class));
        v.setCreatorId(row.get("creator_id", Long.class));
        v.setCreatorName(row.get("creator_name", String.class));
        v.setOperatorId(row.get("operator_id", Long.class));
        v.setOperatorName(row.get("operator_name", String.class));
        Number type = row.get("point_type_flag", Number.class),
                rw = row.get("rw_flag", Number.class),
                en = row.get("enable_flag", Number.class);
        v.setPointTypeFlag(PointTypeEnum.ofIndex(type == null ? null : type.byteValue()));
        v.setRwFlag(RwTypeEnum.ofIndex(rw == null ? null : rw.byteValue()));
        v.setEnableFlag(EnableFlagEnum.ofIndex(en == null ? null : en.byteValue()));
        v.setBaseValue(decimal(row, "base_value"));
        v.setMultiple(decimal(row, "multiple"));
        String ext = row.get("point_ext", String.class);
        if (ext != null)
            try {
                v.setPointExt(objectMapper.readValue(ext, PointExt.class));
            } catch (Exception e) {
                throw new IllegalStateException("point_ext contains invalid JSON", e);
            }
        return v;
    }

    private BigDecimal decimal(io.r2dbc.spi.Row row, String column) {
        BigDecimal value = row.get(column, BigDecimal.class);
        if (value != null) {
            return value;
        }
        Number fallback = row.get(column, Number.class);
        return fallback == null ? null : new BigDecimal(fallback.toString());
    }

    private Byte index(PointTypeEnum value) {
        return value == null ? PointTypeEnum.STRING.getIndex() : value.getIndex();
    }

    private Byte index(RwTypeEnum value) {
        return value == null ? RwTypeEnum.READ_ONLY.getIndex() : value.getIndex();
    }

    private Byte index(EnableFlagEnum value) {
        return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex();
    }

    private String serialize(PointExt value) {
        try {
            JsonExt json = new JsonExt();
            if (value != null) {
                json.setType(value.getType());
                json.setVersion(value.getVersion());
                json.setRemark(value.getRemark());
                json.setContent(value.getContent() == null ? null : JsonUtil.toJsonString(value.getContent()));
            }
            return objectMapper.writeValueAsString(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("point_ext is not valid JSON", e);
        }
    }
}
