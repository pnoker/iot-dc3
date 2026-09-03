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

import io.github.pnoker.common.auth.repository.ReactiveTenantMembershipCommandStore;
import io.github.pnoker.common.auth.repository.ReactiveTenantMembershipStore;
import io.github.pnoker.common.auth.repository.TenantMembershipFilter;

import io.github.pnoker.common.auth.entity.model.TenantMembershipDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.time.Instant;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for active tenant memberships. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcTenantMembershipStore implements ReactiveTenantMembershipStore, ReactiveTenantMembershipCommandStore {

    private static final String TABLE = "dc3_auth.dc3_tenant_membership";
    private static final String COLUMNS =
            "m.id,m.tenant_id,m.principal_id,m.principal_type,m.membership_status,m.joined_time,m.membership_ext,m.remark,m.creator_id,m.creator_name,m.create_time,m.operator_id,m.operator_name,m.operate_time,m.deleted";
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<TenantMembershipDO> getById(Long tenantId, Long id) {
        if (!valid(tenantId, id)) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " m WHERE m.tenant_id=:tenant_id AND m.id=:id AND m.deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<TenantMembershipDO> getByTenantAndPrincipal(Long tenantId, Long principalId) {
        if (!valid(tenantId, principalId)) return Mono.empty();
        return databaseClient
                .sql(
                        "SELECT " + COLUMNS + " FROM " + TABLE
                                + " m WHERE m.tenant_id=:tenant_id AND m.principal_id=:principal_id AND m.membership_status='ACTIVE' AND m.deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("principal_id", principalId)
                .map(this::map)
                .one();
    }

    @Override
    public Flux<Long> listPrincipalIds(Long tenantId) {
        if (!valid(tenantId)) return Flux.empty();
        return databaseClient
                .sql(
                        "SELECT m.principal_id FROM " + TABLE
                                + " m WHERE m.tenant_id=:tenant_id AND m.membership_status='ACTIVE' AND m.deleted=0 ORDER BY m.principal_id")
                .bind("tenant_id", tenantId)
                .map((row, metadata) -> row.get("principal_id", Long.class))
                .all();
    }

    @Override
    public Mono<OffsetPage<TenantMembershipDO>> list(TenantMembershipFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("tenant membership filter is required"));
        StringBuilder where = new StringBuilder(" WHERE m.tenant_id=:tenant_id AND m.deleted=0");
        if (filter.principalId() != null) where.append(" AND m.principal_id=:principal_id");
        if (filter.principalType() != null) where.append(" AND m.principal_type=:principal_type");
        if (filter.membershipStatus() != null) where.append(" AND m.membership_status=:membership_status");
        String condition = where.toString();
        DatabaseClient.GenericExecuteSpec count =
                bind(databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + " m" + condition), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(
                        databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " m" + condition + " ORDER BY "
                                + orderBy(filter.page().sort()) + " LIMIT :limit OFFSET :offset"),
                        filter)
                .bind("limit", filter.page().limit())
                .bind("offset", filter.page().offset());
        Mono<Long> total = count.map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                })
                .one()
                .defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(this::map)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(
                                items, filter.page().offset(), filter.page().limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<TenantMembershipDO> insert(TenantMembershipDO membership) {
        if (membership == null || !valid(membership.getTenantId()) || !valid(membership.getPrincipalId())) {
            return Mono.error(new IllegalArgumentException("tenant membership identifiers are required"));
        }
        if (membership.getId() == null) membership.setId(UuidV7.nextLong());
        if (membership.getDeleted() == null) membership.setDeleted((byte) 0);
        if (membership.getCreateTime() == null) membership.setCreateTime(LocalDateTime.now(ZoneOffset.UTC));
        if (membership.getOperateTime() == null) membership.setOperateTime(membership.getCreateTime());
        if (membership.getJoinedTime() == null) membership.setJoinedTime(membership.getCreateTime());
        DatabaseClient.GenericExecuteSpec statement = databaseClient
                .sql(
                        "INSERT INTO " + TABLE
                                + " (id,tenant_id,principal_id,principal_type,membership_status,joined_time,membership_ext,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                                + " VALUES (:id,:tenant_id,:principal_id,:principal_type,:membership_status,:joined_time,"
                                + dialect.jsonWriteExpression(":membership_ext")
                                + ",:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,:deleted)")
                .bind("id", membership.getId())
                .bind("tenant_id", membership.getTenantId())
                .bind("principal_id", membership.getPrincipalId())
                .bind("joined_time", membership.getJoinedTime())
                .bind("create_time", membership.getCreateTime())
                .bind("operate_time", membership.getOperateTime())
                .bind("deleted", membership.getDeleted());
        statement = bindText(statement, "principal_type", membership.getPrincipalType());
        statement = bindText(statement, "membership_status", membership.getMembershipStatus());
        statement = bindText(statement, "remark", membership.getRemark());
        statement = bindLong(statement, "creator_id", membership.getCreatorId());
        statement = bindText(statement, "creator_name", membership.getCreatorName());
        statement = bindLong(statement, "operator_id", membership.getOperatorId());
        statement = bindText(statement, "operator_name", membership.getOperatorName());
        statement = membership.getMembershipExt() == null
                ? statement.bindNull("membership_ext", String.class)
                : statement.bind("membership_ext", JsonUtil.toJsonString(membership.getMembershipExt()));
        return transactionalOperator.transactional(statement
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? getById(membership.getTenantId(), membership.getId()) : Mono.empty()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (!valid(tenantId, id)) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient
                .sql("UPDATE " + TABLE
                        + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .bind("operator_id", operatorId)
                .bind("operator_name", operatorName)
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1));
    }

    private DatabaseClient.GenericExecuteSpec bind(
            DatabaseClient.GenericExecuteSpec spec, TenantMembershipFilter filter) {
        spec = spec.bind("tenant_id", filter.tenantId());
        if (filter.principalId() != null) spec = spec.bind("principal_id", filter.principalId());
        if (filter.principalType() != null)
            spec = spec.bind("principal_type", filter.principalType().getValue());
        if (filter.membershipStatus() != null)
            spec = spec.bind("membership_status", filter.membershipStatus().getValue());
        return spec;
    }

    private TenantMembershipDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        TenantMembershipDO value = new TenantMembershipDO();
        value.setId(row.get("id", Long.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setPrincipalId(row.get("principal_id", Long.class));
        value.setPrincipalType(row.get("principal_type", String.class));
        value.setMembershipStatus(row.get("membership_status", String.class));
        value.setJoinedTime(time(row.get("joined_time")));
        value.setMembershipExt(json(row.get("membership_ext", String.class)));
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
        if (raw instanceof Instant value) return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
        return null;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "m.principal_id ASC,m.id ASC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column =
                    switch (spec.field()) {
                        case "id" -> "m.id";
                        case "principalId" -> "m.principal_id";
                        case "principalType" -> "m.principal_type";
                        case "membershipStatus" -> "m.membership_status";
                        case "joinedTime" -> "m.joined_time";
                        case "createTime" -> "m.create_time";
                        case "operateTime" -> "m.operate_time";
                        default ->
                            throw new IllegalArgumentException(
                                    "unsupported tenant membership sort field: " + spec.field());
                    };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("m.id "))) clauses.add("m.id ASC");
        return String.join(",", clauses);
    }

    private boolean valid(Long value) {
        return value != null && value > 0;
    }

    private boolean valid(Long tenantId, Long id) {
        return valid(tenantId) && valid(id);
    }

    private DatabaseClient.GenericExecuteSpec bindText(
            DatabaseClient.GenericExecuteSpec spec, String name, String value) {
        return value == null ? spec.bindNull(name, String.class) : spec.bind(name, value);
    }

    private DatabaseClient.GenericExecuteSpec bindLong(
            DatabaseClient.GenericExecuteSpec spec, String name, Long value) {
        return value == null ? spec.bindNull(name, Long.class) : spec.bind(name, value);
    }
}
