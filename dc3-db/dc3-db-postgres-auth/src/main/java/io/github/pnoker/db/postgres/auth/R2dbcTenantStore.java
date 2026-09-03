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
package io.github.pnoker.db.postgres.auth;

import io.github.pnoker.common.auth.repository.ReactiveTenantStore;
import io.github.pnoker.common.auth.repository.TenantFilter;

import io.github.pnoker.common.auth.entity.model.TenantDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for {@code dc3_auth.dc3_tenant}. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcTenantStore implements ReactiveTenantStore {

    private static final String TABLE = "dc3_auth.dc3_tenant";
    private static final String COLUMNS =
            "id,tenant_name,tenant_code,tenant_ext,enable_flag,remark,creator_id,creator_name,"
                    + "create_time,operator_id,operator_name,operate_time,deleted";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<TenantDO> getById(Long id) {
        if (id == null || id <= 0) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE id=:id AND deleted=0 LIMIT 1")
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<TenantDO> getEnabledByCode(String code) {
        if (code == null || code.isBlank()) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_code=:tenant_code AND enable_flag=0 AND deleted=0 LIMIT 1")
                .bind("tenant_code", code.trim())
                .map(this::map)
                .one();
    }

    @Override
    public Mono<TenantDO> getByNameAndCode(String tenantName, String tenantCode) {
        if (tenantName == null || tenantName.isBlank() || tenantCode == null || tenantCode.isBlank()) {
            return Mono.empty();
        }
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_name=:tenant_name AND tenant_code=:tenant_code AND deleted=0 LIMIT 1")
                .bind("tenant_name", tenantName.trim())
                .bind("tenant_code", tenantCode.trim())
                .map(this::map)
                .one();
    }

    @Override
    public Mono<OffsetPage<TenantDO>> list(TenantFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("tenant filter is required"));
        StringBuilder where = new StringBuilder(" WHERE deleted=0");
        if (filter.tenantName() != null) where.append(" AND tenant_name LIKE :tenant_name");
        if (filter.tenantCode() != null) where.append(" AND tenant_code=:tenant_code");
        if (filter.enableFlag() != null) where.append(" AND enable_flag=:enable_flag");
        String condition = where.toString();
        DatabaseClient.GenericExecuteSpec count =
                databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + condition);
        DatabaseClient.GenericExecuteSpec rows = databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + condition + " ORDER BY "
                        + orderBy(filter.page().sort()) + " LIMIT :limit OFFSET :offset")
                .bind("limit", filter.page().limit())
                .bind("offset", filter.page().offset());
        if (filter.tenantName() != null) {
            String value = "%" + filter.tenantName() + "%";
            count = count.bind("tenant_name", value);
            rows = rows.bind("tenant_name", value);
        }
        if (filter.tenantCode() != null) {
            count = count.bind("tenant_code", filter.tenantCode());
            rows = rows.bind("tenant_code", filter.tenantCode());
        }
        if (filter.enableFlag() != null) {
            count = count.bind("enable_flag", filter.enableFlag().getIndex());
            rows = rows.bind("enable_flag", filter.enableFlag().getIndex());
        }
        Mono<Long> total = count.map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                })
                .one()
                .defaultIfEmpty(0L);
        DatabaseClient.GenericExecuteSpec itemRows = rows;
        return total.flatMap(totalCount -> itemRows.map(this::map)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(
                                items, filter.page().offset(), filter.page().limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<TenantDO> insert(TenantDO tenant) {
        if (tenant.getId() == null) tenant.setId(UuidV7.nextLong());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DatabaseClient.GenericExecuteSpec insert = databaseClient
                .sql(
                        "INSERT INTO " + TABLE + " (" + COLUMNS
                                + ") VALUES (:id,:tenant_name,:tenant_code,"
                                + dialect.jsonWriteExpression(":tenant_ext")
                                + ",:enable_flag,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", tenant.getId())
                .bind("tenant_name", text(tenant.getTenantName()))
                .bind("tenant_code", text(tenant.getTenantCode()))
                .bind("enable_flag", flag(tenant.getEnableFlag()))
                .bind("remark", text(tenant.getRemark()))
                .bind("creator_id", value(tenant.getCreatorId()))
                .bind("creator_name", text(tenant.getCreatorName()))
                .bind("create_time", now)
                .bind("operator_id", value(tenant.getOperatorId()))
                .bind("operator_name", text(tenant.getOperatorName()))
                .bind("operate_time", now);
        insert = bindJson(insert, "tenant_ext", tenant.getTenantExt());
        return transactionalOperator.transactional(insert.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? getById(tenant.getId())
                        : Mono.error(new IllegalStateException("tenant insert affected " + rows + " rows"))));
    }

    @Override
    public Mono<TenantDO> update(TenantDO tenant) {
        if (tenant.getId() == null) return Mono.empty();
        DatabaseClient.GenericExecuteSpec update = databaseClient
                .sql("UPDATE " + TABLE + " SET tenant_name=:tenant_name,"
                        + "tenant_code=:tenant_code,tenant_ext=" + dialect.jsonWriteExpression(":tenant_ext")
                        + ",enable_flag=:enable_flag,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,"
                        + "operate_time=:operate_time WHERE id=:id AND deleted=0")
                .bind("tenant_name", text(tenant.getTenantName()))
                .bind("tenant_code", text(tenant.getTenantCode()))
                .bind("enable_flag", flag(tenant.getEnableFlag()))
                .bind("remark", text(tenant.getRemark()))
                .bind("operator_id", value(tenant.getOperatorId()))
                .bind("operator_name", text(tenant.getOperatorName()))
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .bind("id", tenant.getId());
        update = bindJson(update, "tenant_ext", tenant.getTenantExt());
        return transactionalOperator.transactional(
                update.fetch().rowsUpdated().flatMap(rows -> rows == 1 ? getById(tenant.getId()) : Mono.empty()));
    }

    @Override
    public Mono<Boolean> delete(Long id, Long operatorId, String operatorName) {
        if (id == null || id <= 0) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient
                .sql("UPDATE " + TABLE
                        + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                        + " WHERE id=:id AND deleted=0")
                .bind("id", id)
                .bind("operator_id", value(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1));
    }

    private TenantDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        TenantDO value = new TenantDO();
        value.setId(row.get("id", Long.class));
        value.setTenantName(row.get("tenant_name", String.class));
        value.setTenantCode(row.get("tenant_code", String.class));
        value.setTenantExt(json(row.get("tenant_ext", String.class)));
        Number enable = row.get("enable_flag", Number.class);
        value.setEnableFlag(enable == null ? null : enable.byteValue());
        value.setRemark(row.get("remark", String.class));
        value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class));
        value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(time(row.get("operate_time")));
        Number deleted = row.get("deleted", Number.class);
        value.setDeleted(deleted == null ? null : deleted.byteValue());
        return value;
    }

    private JsonExt json(String raw) {
        if (raw == null) return null;
        try {
            return JsonUtil.parseObject(raw, JsonExt.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private LocalDateTime time(Object raw) {
        if (raw instanceof LocalDateTime value) return value;
        if (raw instanceof OffsetDateTime value)
            return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (raw instanceof java.time.Instant value) return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
        return null;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "tenant_name ASC,id ASC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column =
                    switch (spec.field()) {
                        case "id" -> "id";
                        case "tenantName" -> "tenant_name";
                        case "tenantCode" -> "tenant_code";
                        case "createTime" -> "create_time";
                        case "operateTime" -> "operate_time";
                        default -> throw new IllegalArgumentException("unsupported tenant sort field: " + spec.field());
                    };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("id "))) clauses.add("id ASC");
        return String.join(",", clauses);
    }

    private DatabaseClient.GenericExecuteSpec bindJson(
            DatabaseClient.GenericExecuteSpec spec, String name, Object value) {
        String json = value == null ? "{}" : JsonUtil.toJsonString(value);
        return spec.bind(name, json);
    }

    private byte flag(Byte value) {
        return value == null ? EnableFlagEnum.ENABLE.getIndex() : value;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
