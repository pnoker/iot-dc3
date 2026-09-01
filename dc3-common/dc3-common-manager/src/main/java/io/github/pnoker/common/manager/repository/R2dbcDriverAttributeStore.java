package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.entity.ext.DriverAttributeExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
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
import java.util.Collection;
import java.util.List;

/** Explicit SQL adapter for manager driver attributes. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcDriverAttributeStore implements ReactiveDriverAttributeStore {
    private static final String TABLE = "dc3_manager.dc3_driver_attribute";
    private static final String COLUMNS = "id, attribute_name, attribute_code, attribute_type_flag, default_value, driver_id, attribute_ext, enable_flag, tenant_id, remark, signature, version, creator_id, creator_name, create_time, operator_id, operator_name, operate_time";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final ObjectMapper objectMapper;
    private final R2dbcDialect dialect;

    @Override
    public Mono<DriverAttributeBO> get(Long tenantId, Long id) {
        if (tenantId == null || id == null || tenantId <= 0 || id <= 0) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId).bind("id", id).map(this::map).one();
    }

    @Override
    public Mono<DriverAttributeBO> getByCodeAndDriver(Long tenantId, String attributeCode, Long driverId) {
        if (tenantId == null || driverId == null || attributeCode == null || attributeCode.isBlank()) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND driver_id=:driver_id AND attribute_code=:attribute_code AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId).bind("driver_id", driverId).bind("attribute_code", attributeCode)
                .map(this::map).one();
    }

    @Override
    public Flux<DriverAttributeBO> listByDriverId(Long tenantId, Long driverId) {
        if (tenantId == null || driverId == null || tenantId <= 0 || driverId <= 0) return Flux.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND driver_id=:driver_id AND deleted=0 ORDER BY id ASC")
                .bind("tenant_id", tenantId).bind("driver_id", driverId).map(this::map).all();
    }

    @Override
    public Mono<DriverAttributeBO> insert(DriverAttributeBO value) {
        if (value == null || value.getTenantId() == null || value.getDriverId() == null
                || value.getAttributeName() == null || value.getAttributeName().isBlank()
                || value.getAttributeCode() == null || value.getAttributeCode().isBlank()) {
            return Mono.error(new IllegalArgumentException("tenantId, driverId, attributeName and attributeCode are required"));
        }
        if (value.getId() == null) value.setId(UuidV7.nextLong());
        if (value.getVersion() == null) value.setVersion(0);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("INSERT INTO " + TABLE
                        + " (" + COLUMNS + ", deleted) VALUES (:id,:attribute_name,:attribute_code,:attribute_type_flag,:default_value,:driver_id," + dialect.jsonWriteExpression(":attribute_ext") + ",:enable_flag,:tenant_id,:remark,:signature,:version,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", value.getId()).bind("attribute_name", value.getAttributeName().trim())
                .bind("attribute_code", value.getAttributeCode().trim()).bind("attribute_type_flag", index(value.getAttributeTypeFlag()))
                .bind("default_value", valueOrEmpty(value.getDefaultValue())).bind("driver_id", value.getDriverId())
                .bind("attribute_ext", serialize(value.getAttributeExt())).bind("enable_flag", index(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId()).bind("remark", valueOrEmpty(value.getRemark()))
                .bind("signature", valueOrEmpty(value.getSignature())).bind("version", value.getVersion())
                .bind("creator_name", valueOrEmpty(value.getCreatorName())).bind("create_time", now)
                .bind("operator_name", valueOrEmpty(value.getOperatorName())).bind("operate_time", now)
                .bind("creator_id", value.getCreatorId() == null ? 0L : value.getCreatorId())
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId());
        return transactionalOperator.transactional(spec.fetch().rowsUpdated().then(get(value.getTenantId(), value.getId())));
    }

    @Override
    public Mono<DriverAttributeBO> update(DriverAttributeBO value, int expectedVersion) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("UPDATE " + TABLE
                        + " SET attribute_name=:attribute_name, attribute_code=:attribute_code, attribute_type_flag=:attribute_type_flag, default_value=:default_value, driver_id=:driver_id, attribute_ext=" + dialect.jsonWriteExpression(":attribute_ext") + ", enable_flag=:enable_flag, remark=:remark, signature=:signature, version=version+1, operator_id=:operator_id, operator_name=:operator_name, operate_time=:operate_time WHERE id=:id AND tenant_id=:tenant_id AND version=:expected_version AND deleted=0")
                .bind("attribute_name", valueOrEmpty(value.getAttributeName())).bind("attribute_code", valueOrEmpty(value.getAttributeCode()))
                .bind("attribute_type_flag", index(value.getAttributeTypeFlag())).bind("default_value", valueOrEmpty(value.getDefaultValue()))
                .bind("driver_id", value.getDriverId()).bind("attribute_ext", serialize(value.getAttributeExt()))
                .bind("enable_flag", index(value.getEnableFlag())).bind("remark", valueOrEmpty(value.getRemark()))
                .bind("signature", valueOrEmpty(value.getSignature())).bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", valueOrEmpty(value.getOperatorName())).bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .bind("id", value.getId()).bind("tenant_id", value.getTenantId()).bind("expected_version", expectedVersion);
        return transactionalOperator.transactional(spec.fetch().rowsUpdated().flatMap(rows -> rows == 1
                ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient.sql("UPDATE " + TABLE
                        + " SET deleted=1, operator_id=:operator_id, operator_name=:operator_name, operate_time=:operate_time"
                        + " WHERE tenant_id=:tenant_id AND id=:id AND version=:expected_version AND deleted=0")
                .bind("tenant_id", tenantId).bind("id", id).bind("expected_version", expectedVersion)
                .bind("operator_id", operatorId == null ? 0L : operatorId)
                .bind("operator_name", valueOrEmpty(operatorName)).bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .fetch().rowsUpdated().map(rows -> rows == 1));
    }

    @Override
    public Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName) {
        List<Long> values = ids == null ? List.of() : ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (tenantId == null || tenantId <= 0 || values.isEmpty()) return Mono.just(false);
        return deleteSql(tenantId, values, operatorId, operatorName);
    }

    private Mono<Boolean> deleteSql(Long tenantId, List<Long> ids, Long operatorId, String operatorName) {
        String placeholders = java.util.stream.IntStream.range(0, ids.size()).mapToObj(index -> ":id" + index)
                .reduce((left, right) -> left + "," + right).orElseThrow();
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("UPDATE " + TABLE
                        + " SET deleted=1, operator_id=:operator_id, operator_name=:operator_name, operate_time=:operate_time WHERE tenant_id=:tenant_id AND id IN (" + placeholders + ") AND deleted=0")
                .bind("tenant_id", tenantId).bind("operator_id", operatorId == null ? 0L : operatorId)
                .bind("operator_name", valueOrEmpty(operatorName)).bind("operate_time", LocalDateTime.now(ZoneOffset.UTC));
        for (int index = 0; index < ids.size(); index++) spec = spec.bind("id" + index, ids.get(index));
        return transactionalOperator.transactional(spec.fetch().rowsUpdated().map(rows -> rows > 0));
    }

    @Override
    public Mono<OffsetPage<DriverAttributeBO>> list(DriverAttributeFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("filter is required"));
        StringBuilder where = new StringBuilder(" WHERE a.tenant_id=:tenant_id AND a.deleted=0");
        if (present(filter.attributeName())) where.append(" AND a.attribute_name LIKE :attribute_name");
        if (present(filter.attributeCode())) where.append(" AND a.attribute_code=:attribute_code");
        if (filter.attributeTypeFlag() != null) where.append(" AND a.attribute_type_flag=:attribute_type_flag");
        if (filter.driverId() != null) where.append(" AND a.driver_id=:driver_id");
        if (filter.enableFlag() != null) where.append(" AND a.enable_flag=:enable_flag");
        if (filter.version() != null) where.append(" AND a.version=:version");
        DatabaseClient.GenericExecuteSpec count = databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + " a" + where)
                .bind("tenant_id", filter.tenantId());
        DatabaseClient.GenericExecuteSpec items = databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " a" + where
                        + orderBy(filter.sort()) + " LIMIT :limit OFFSET :offset")
                .bind("tenant_id", filter.tenantId()).bind("limit", filter.limit()).bind("offset", filter.offset());
        count = bind(count, filter); items = bind(items, filter);
        Mono<Long> total = count.map((row, metadata) -> row.get("total", Long.class)).one().defaultIfEmpty(0L);
        DatabaseClient.GenericExecuteSpec itemRows = items;
        return total.flatMap(totalCount -> itemRows.map(this::map).all().collectList()
                        .map(pageItems -> OffsetPage.of(pageItems, filter.offset(), filter.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, DriverAttributeFilter filter) {
        if (present(filter.attributeName())) spec = spec.bind("attribute_name", "%" + filter.attributeName().trim() + "%");
        if (present(filter.attributeCode())) spec = spec.bind("attribute_code", filter.attributeCode().trim());
        if (filter.attributeTypeFlag() != null) spec = spec.bind("attribute_type_flag", filter.attributeTypeFlag().getIndex());
        if (filter.driverId() != null) spec = spec.bind("driver_id", filter.driverId());
        if (filter.enableFlag() != null) spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        if (filter.version() != null) spec = spec.bind("version", filter.version());
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        List<String> clauses = new ArrayList<>();
        if (sort != null) for (SortSpec spec : sort) {
            String column = switch (spec.field()) {
                case "id" -> "a.id"; case "attributeName" -> "a.attribute_name";
                case "attributeCode" -> "a.attribute_code"; case "driverId" -> "a.driver_id";
                case "createTime" -> "a.create_time"; case "operateTime" -> "a.operate_time";
                case "version" -> "a.version"; default -> throw new IllegalArgumentException("unsupported driver attribute sort field");
            };
            clauses.add(column + (spec.direction() == SortSpec.Direction.DESC ? " DESC" : " ASC"));
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("a.id"))) clauses.add("a.id ASC");
        return " ORDER BY " + String.join(", ", clauses);
    }

    private DriverAttributeBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        DriverAttributeBO value = new DriverAttributeBO();
        value.setId(row.get("id", Long.class)); value.setAttributeName(row.get("attribute_name", String.class));
        value.setAttributeCode(row.get("attribute_code", String.class)); value.setDefaultValue(row.get("default_value", String.class));
        value.setDriverId(row.get("driver_id", Long.class)); value.setTenantId(row.get("tenant_id", Long.class));
        value.setRemark(row.get("remark", String.class)); value.setSignature(row.get("signature", String.class));
        value.setVersion(row.get("version", Integer.class)); value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class)); value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class)); value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(time(row.get("operate_time")));
        Number type = row.get("attribute_type_flag", Number.class), enabled = row.get("enable_flag", Number.class);
        value.setAttributeTypeFlag(AttributeTypeEnum.ofIndex(type == null ? null : type.byteValue()));
        value.setEnableFlag(EnableFlagEnum.ofIndex(enabled == null ? null : enabled.byteValue()));
        String raw = row.get("attribute_ext", String.class);
        if (raw != null) try {
            JsonExt json = objectMapper.readValue(raw, JsonExt.class);
            DriverAttributeExt ext = new DriverAttributeExt(); ext.setType(json.getType()); ext.setVersion(json.getVersion()); ext.setRemark(json.getRemark());
            ext.setContent(json.getContent() == null ? null : JsonUtil.parseObject(json.getContent(), DriverAttributeExt.Content.class));
            value.setAttributeExt(ext);
        } catch (Exception exception) { throw new IllegalStateException("attribute_ext contains invalid JSON", exception); }
        return value;
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset) return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return null;
    }
    private Byte index(AttributeTypeEnum value) { return value == null ? AttributeTypeEnum.STRING.getIndex() : value.getIndex(); }
    private Byte index(EnableFlagEnum value) { return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex(); }
    private String valueOrEmpty(String value) { return value == null ? "" : value; }
    private String serialize(DriverAttributeExt value) {
        try { JsonExt json = new JsonExt(); if (value != null) { json.setType(value.getType()); json.setVersion(value.getVersion()); json.setRemark(value.getRemark()); json.setContent(value.getContent() == null ? null : JsonUtil.toJsonString(value.getContent())); } return objectMapper.writeValueAsString(json); }
        catch (Exception exception) { throw new IllegalArgumentException("attribute_ext is not valid JSON", exception); }
    }
    private boolean present(String value) { return value != null && !value.isBlank(); }
}
