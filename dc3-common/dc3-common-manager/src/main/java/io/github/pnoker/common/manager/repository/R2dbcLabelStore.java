package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.LabelBO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
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
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** R2DBC label repository shared by PostgreSQL, MySQL and MariaDB. */
@Repository
@RequiredArgsConstructor
@ConditionalOnClass({DatabaseClient.class, R2dbcDialect.class, TransactionalOperator.class})
public class R2dbcLabelStore implements ReactiveLabelStore {
    private static final String TABLE = "dc3_manager.dc3_label";
    private static final String COLUMNS = "id,label_name,label_code,label_color,entity_type_flag,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time";
    private final DatabaseClient client;
    private final R2dbcDialect dialect;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;

    @Override
    public Mono<OffsetPage<LabelBO>> list(LabelFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE tenant_id=:tenant_id AND deleted=0");
        if (text(filter.labelName())) where.append(" AND ").append(dialect.caseInsensitiveLike("label_name", ":label_name"));
        if (text(filter.color())) where.append(" AND label_color=:color");
        if (filter.entityTypeFlag() != null) where.append(" AND entity_type_flag=:entity_type");
        if (filter.enableFlag() != null) where.append(" AND enable_flag=:enable_flag");
        DatabaseClient.GenericExecuteSpec rows = bind(client.sql("SELECT " + COLUMNS + " FROM " + TABLE + where
                        + " ORDER BY " + orderBy(filter.sort()) + " LIMIT :limit OFFSET :offset"), filter)
                .bind("limit", filter.limit()).bind("offset", filter.offset());
        Mono<List<LabelBO>> items = rows.map(this::map).all().collectList();
        Mono<Long> total = bind(client.sql("SELECT COUNT(*) AS total FROM " + TABLE + where), filter)
                .map((row, metadata) -> number(row.get("total")).longValue()).one().defaultIfEmpty(0L);
        return total.flatMap(totalCount -> items.map(pageItems -> new OffsetPage<>(pageItems, filter.offset(),
                        filter.limit(), totalCount, filter.offset() + pageItems.size() < totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<LabelBO> get(Long tenantId, Long id) {
        if (!valid(tenantId) || !valid(id)) return Mono.empty();
        return client.sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId).bind("id", id).map(this::map).one();
    }

    @Override
    public Mono<LabelBO> getByName(Long tenantId, String name, byte entityType) {
        if (!valid(tenantId) || !text(name)) return Mono.empty();
        return client.sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND label_name=:label_name AND entity_type_flag=:entity_type AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId).bind("label_name", name).bind("entity_type", entityType).map(this::map).one();
    }

    @Override
    public Mono<Boolean> hasActiveBindings(Long tenantId, Long labelId) {
        if (!valid(tenantId) || !valid(labelId)) return Mono.just(false);
        return client.sql("SELECT 1 FROM dc3_manager.dc3_label_bind WHERE tenant_id=:tenant_id AND label_id=:label_id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId).bind("label_id", labelId).map((row, metadata) -> true).one().defaultIfEmpty(false);
    }

    @Override
    public Mono<LabelBO> insert(LabelBO value) {
        value.setId(value.getId() == null ? UuidV7.nextLong() : value.getId());
        value.setCreateTime(now()); value.setOperateTime(value.getCreateTime());
        String sql = "INSERT INTO " + TABLE + " (id,label_name,label_code,label_color,entity_type_flag,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:label_name,:label_code,:label_color,:entity_type,:enable_flag,:tenant_id,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        return transactionalOperator.transactional(write(sql, value).fetch().rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }

    @Override
    public Mono<LabelBO> update(LabelBO value) {
        value.setOperateTime(now());
        String sql = "UPDATE " + TABLE + " SET label_name=:label_name,label_code=:label_code,label_color=:label_color,entity_type_flag=:entity_type,enable_flag=:enable_flag,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0";
        return transactionalOperator.transactional(write(sql, value).fetch().rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (!valid(tenantId) || !valid(id)) return Mono.just(false);
        return transactionalOperator.transactional(client.sql("UPDATE " + TABLE + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0")
                .bind("operator_id", operatorId == null ? 0L : operatorId).bind("operator_name", operatorName == null ? "" : operatorName)
                .bind("operate_time", dialect.bindInstant(Instant.now())).bind("tenant_id", tenantId).bind("id", id)
                .fetch().rowsUpdated().map(rows -> rows == 1));
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, LabelFilter filter) {
        spec = spec.bind("tenant_id", filter.tenantId());
        if (text(filter.labelName())) spec = spec.bind("label_name", "%" + filter.labelName().trim() + "%");
        if (text(filter.color())) spec = spec.bind("color", filter.color().trim());
        if (filter.entityTypeFlag() != null) spec = spec.bind("entity_type", filter.entityTypeFlag().getIndex());
        if (filter.enableFlag() != null) spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec write(String sql, LabelBO value) {
        DatabaseClient.GenericExecuteSpec spec = client.sql(sql).bind("id", value.getId()).bind("label_name", value.getLabelName())
                .bind("label_code", value.getLabelCode()).bind("label_color", value.getLabelColor())
                .bind("entity_type", value.getEntityTypeFlag().getIndex()).bind("enable_flag", flag(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId()).bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", value.getOperatorName() == null ? "" : value.getOperatorName()).bind("operate_time", dialect.bindInstant(toInstant(value.getOperateTime())));
        if (sql.startsWith("INSERT")) {
            spec = spec.bind("creator_id", value.getCreatorId() == null ? 0L : value.getCreatorId())
                    .bind("creator_name", value.getCreatorName() == null ? "" : value.getCreatorName())
                    .bind("create_time", dialect.bindInstant(toInstant(value.getCreateTime())));
        }
        return spec;
    }

    private LabelBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        LabelBO value = new LabelBO(); value.setId(row.get("id", Long.class)); value.setLabelName(row.get("label_name", String.class));
        value.setLabelCode(row.get("label_code", String.class)); value.setLabelColor(row.get("label_color", String.class));
        value.setEntityTypeFlag(EntityTypeEnum.ofIndex(number(row.get("entity_type_flag")).byteValue()));
        value.setEnableFlag(EnableFlagEnum.ofIndex(number(row.get("enable_flag")).byteValue())); value.setTenantId(row.get("tenant_id", Long.class));
        value.setRemark(row.get("remark", String.class)); value.setCreatorId(row.get("creator_id", Long.class)); value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(time(row.get("create_time"))); value.setOperatorId(row.get("operator_id", Long.class)); value.setOperatorName(row.get("operator_name", String.class)); value.setOperateTime(time(row.get("operate_time"))); return value;
    }

    private String orderBy(List<SortSpec> sort) { List<String> clauses = new ArrayList<>(); for (SortSpec spec : sort) { String column = switch (spec.field()) { case "labelName" -> "label_name"; case "labelCode" -> "label_code"; case "createTime" -> "create_time"; case "operateTime" -> "operate_time"; case "id" -> "id"; default -> throw new IllegalArgumentException("label sort field is not allowed"); }; clauses.add(column + (spec.direction() == SortSpec.Direction.ASC ? " ASC" : " DESC")); } if (clauses.stream().noneMatch(v -> v.startsWith("id "))) clauses.add("id DESC"); return String.join(", ", clauses); }
    private LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private Instant toInstant(LocalDateTime value) { return value.toInstant(ZoneOffset.UTC); }
    private LocalDateTime time(Object value) { if (value instanceof LocalDateTime local) return local; if (value instanceof OffsetDateTime offset) return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime(); if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC); return null; }
    private Number number(Object value) { return value instanceof Number number ? number : 0; }
    private byte flag(EnableFlagEnum value) { return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex(); }
    private boolean text(String value) { return value != null && !value.isBlank(); }
    private boolean valid(Long value) { return value != null && value > 0; }
}
