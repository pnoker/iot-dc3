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
package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for {@code dc3_auth.dc3_principal}. */
@Repository
@ConditionalOnClass({DatabaseClient.class, PageTransaction.class})
@RequiredArgsConstructor
public class R2dbcPrincipalStore implements ReactivePrincipalStore {

    private static final String TABLE = "dc3_auth.dc3_principal";
    private static final String COLUMNS = "id,principal_type,principal_name,display_name,source_type,enable_flag,"
            + "locked_flag,last_login_time,principal_ext,remark,creator_id,creator_name,create_time,operator_id,"
            + "operator_name,operate_time,deleted";

    private final DatabaseClient databaseClient;
    private final PageTransaction pageTransaction;

    @Override
    public Mono<PrincipalDO> getById(Long tenantId, Long id) {
        if (tenantId == null || tenantId <= 0 || id == null || id <= 0) return Mono.empty();
        return databaseClient
                .sql(
                        "SELECT " + COLUMNS + " FROM " + TABLE
                                + " p WHERE p.id=:id AND p.deleted=0 AND EXISTS (SELECT 1 FROM dc3_auth.dc3_tenant_membership m"
                                + " WHERE m.tenant_id=:tenant_id AND m.principal_id=p.id AND m.membership_status='ACTIVE' AND m.deleted=0) LIMIT 1")
                .bind("id", id)
                .bind("tenant_id", tenantId)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<OffsetPage<PrincipalDO>> list(Long tenantId, PrincipalFilter filter) {
        if (tenantId == null || tenantId <= 0) return Mono.error(new IllegalArgumentException("tenant id is required"));
        if (filter == null) return Mono.error(new IllegalArgumentException("principal filter is required"));
        StringBuilder where = new StringBuilder(
                " p WHERE p.deleted=0 AND EXISTS (SELECT 1 FROM dc3_auth.dc3_tenant_membership m"
                        + " WHERE m.tenant_id=:tenant_id AND m.principal_id=p.id AND m.membership_status='ACTIVE' AND m.deleted=0)");
        if (filter.principalType() != null) where.append(" AND principal_type=:principal_type");
        if (filter.principalName() != null) where.append(" AND principal_name LIKE :principal_name");
        if (filter.displayName() != null) where.append(" AND display_name LIKE :display_name");
        if (filter.sourceType() != null) where.append(" AND source_type=:source_type");
        if (filter.enableFlag() != null) where.append(" AND enable_flag=:enable_flag");
        String condition = where.toString();
        DatabaseClient.GenericExecuteSpec count = databaseClient
                .sql("SELECT COUNT(*) AS total FROM " + TABLE + condition)
                .bind("tenant_id", tenantId);
        DatabaseClient.GenericExecuteSpec rows = databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + condition + " ORDER BY "
                        + orderBy(filter.page().sort()) + " LIMIT :limit OFFSET :offset")
                .bind("tenant_id", tenantId)
                .bind("limit", filter.page().limit())
                .bind("offset", filter.page().offset());
        if (filter.principalType() != null) {
            count = count.bind("principal_type", filter.principalType().getValue());
            rows = rows.bind("principal_type", filter.principalType().getValue());
        }
        if (filter.principalName() != null) {
            String value = "%" + filter.principalName() + "%";
            count = count.bind("principal_name", value);
            rows = rows.bind("principal_name", value);
        }
        if (filter.displayName() != null) {
            String value = "%" + filter.displayName() + "%";
            count = count.bind("display_name", value);
            rows = rows.bind("display_name", value);
        }
        if (filter.sourceType() != null) {
            count = count.bind("source_type", filter.sourceType().getValue());
            rows = rows.bind("source_type", filter.sourceType().getValue());
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
    public Flux<PrincipalDO> listByIds(Long tenantId, Collection<Long> ids) {
        if (tenantId == null || tenantId <= 0) return Flux.empty();
        if (ids == null || ids.isEmpty()) return Flux.empty();
        List<Long> values =
                ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (values.isEmpty()) return Flux.empty();
        String placeholders = java.util.stream.IntStream.range(0, values.size())
                .mapToObj(index -> ":id" + index)
                .reduce((left, right) -> left + "," + right)
                .orElseThrow();
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(
                        "SELECT " + COLUMNS + " FROM " + TABLE
                                + " p WHERE p.id IN (" + placeholders
                                + ") AND p.deleted=0 AND EXISTS (SELECT 1 FROM dc3_auth.dc3_tenant_membership m"
                                + " WHERE m.tenant_id=:tenant_id AND m.principal_id=p.id AND m.membership_status='ACTIVE' AND m.deleted=0) ORDER BY p.id")
                .bind("tenant_id", tenantId);
        for (int index = 0; index < values.size(); index++) query = query.bind("id" + index, values.get(index));
        return query.map(this::map).all();
    }

    @Override
    public Mono<PrincipalDO> updateEnableFlag(
            Long tenantId, Long id, byte enableFlag, Long operatorId, String operatorName) {
        if (tenantId == null || tenantId <= 0 || id == null || id <= 0) return Mono.empty();
        return databaseClient
                .sql("UPDATE " + TABLE + " SET enable_flag=:enable_flag,operator_id=:operator_id,"
                        + "operator_name=:operator_name,operate_time=:operate_time WHERE id=:id AND deleted=0"
                        + " AND EXISTS (SELECT 1 FROM dc3_auth.dc3_tenant_membership m WHERE m.tenant_id=:tenant_id"
                        + " AND m.principal_id=dc3_principal.id AND m.membership_status='ACTIVE' AND m.deleted=0)")
                .bind("enable_flag", enableFlag)
                .bind("operator_id", value(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? getById(tenantId, id) : Mono.empty());
    }

    @Override
    public Mono<Boolean> touchLastLogin(Long id) {
        if (id == null || id <= 0) return Mono.just(false);
        return databaseClient
                .sql("UPDATE " + TABLE + " SET last_login_time=:last_login_time,"
                        + "operate_time=:operate_time WHERE id=:id AND deleted=0")
                .bind("last_login_time", LocalDateTime.now(ZoneOffset.UTC))
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1);
    }

    private PrincipalDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        PrincipalDO value = new PrincipalDO();
        value.setId(row.get("id", Long.class));
        value.setPrincipalType(row.get("principal_type", String.class));
        value.setPrincipalName(row.get("principal_name", String.class));
        value.setDisplayName(row.get("display_name", String.class));
        value.setSourceType(row.get("source_type", String.class));
        Number enable = row.get("enable_flag", Number.class);
        value.setEnableFlag(enable == null ? null : enable.byteValue());
        Number locked = row.get("locked_flag", Number.class);
        value.setLockedFlag(locked == null ? null : locked.byteValue());
        value.setLastLoginTime(time(row.get("last_login_time")));
        value.setPrincipalExt(json(row.get("principal_ext", String.class)));
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
        if (sort == null || sort.isEmpty()) return "principal_name ASC,id ASC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column =
                    switch (spec.field()) {
                        case "id" -> "id";
                        case "principalName" -> "principal_name";
                        case "displayName" -> "display_name";
                        case "principalType" -> "principal_type";
                        case "sourceType" -> "source_type";
                        case "lastLoginTime" -> "last_login_time";
                        case "createTime" -> "create_time";
                        case "operateTime" -> "operate_time";
                        default ->
                            throw new IllegalArgumentException("unsupported principal sort field: " + spec.field());
                    };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("id "))) clauses.add("id ASC");
        return String.join(",", clauses);
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
