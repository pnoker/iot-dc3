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

import io.github.pnoker.common.auth.repository.ReactiveServiceAccountStore;
import io.github.pnoker.common.auth.repository.ServiceAccountFilter;

import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.entity.model.ServiceAccountDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
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
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for tenant-scoped service-account aggregates. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcServiceAccountStore implements ReactiveServiceAccountStore {

    private static final String TABLE = "dc3_auth.dc3_service_account";
    private static final String PRINCIPAL = "dc3_auth.dc3_principal";
    private static final String MEMBERSHIP = "dc3_auth.dc3_tenant_membership";
    private static final String COLUMNS = "id,principal_id,tenant_id,service_account_name,owner_principal_id,purpose,"
            + "expire_time,last_used_time,credential_policy_ext,enable_flag,remark,creator_id,creator_name,create_time,"
            + "operator_id,operator_name,operate_time,deleted";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<ServiceAccountDO> getById(Long tenantId, Long id) {
        if (!valid(tenantId, id)) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<OffsetPage<ServiceAccountDO>> list(ServiceAccountFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("service account filter is required"));
        StringBuilder where =
                new StringBuilder(" FROM ").append(TABLE).append(" WHERE tenant_id=:tenant_id AND deleted=0");
        if (filter.principalId() != null) where.append(" AND principal_id=:principal_id");
        if (filter.serviceAccountName() != null) where.append(" AND service_account_name LIKE :service_account_name");
        if (filter.ownerPrincipalId() != null) where.append(" AND owner_principal_id=:owner_principal_id");
        if (filter.enableFlag() != null) where.append(" AND enable_flag=:enable_flag");
        DatabaseClient.GenericExecuteSpec count = bind(databaseClient.sql("SELECT COUNT(*) AS total" + where), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(
                        databaseClient.sql("SELECT " + COLUMNS + where + " ORDER BY "
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
    public Mono<ServiceAccountDO> insert(ServiceAccountBO account) {
        if (account == null || !valid(account.getTenantId(), account.getOwnerPrincipalId())) {
            return Mono.error(new IllegalArgumentException("service account identifiers are required"));
        }
        long serviceAccountId = id();
        long principalId = id();
        long membershipId = id();
        LocalDateTime now = now();
        byte enableFlag = flag(account.getEnableFlag());
        String name = text(account.getServiceAccountName());
        Mono<Void> principal = databaseClient
                .sql(
                        "INSERT INTO " + PRINCIPAL
                                + " (id,principal_type,principal_name,display_name,source_type,enable_flag,locked_flag,principal_ext,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                                + " VALUES (:id,'SERVICE_ACCOUNT',:principal_name,:display_name,'LOCAL',:enable_flag,0,"
                                + dialect.jsonWriteExpression(":principal_ext")
                                + ",:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", principalId)
                .bind("principal_name", account.getTenantId() + ":" + name)
                .bind("display_name", name)
                .bind("enable_flag", enableFlag)
                .bind("principal_ext", "{}")
                .bind("remark", text(account.getRemark()))
                .bind("creator_id", value(account.getCreatorId()))
                .bind("creator_name", text(account.getCreatorName()))
                .bind("create_time", now)
                .bind("operator_id", value(account.getOperatorId()))
                .bind("operator_name", text(account.getOperatorName()))
                .bind("operate_time", now)
                .fetch()
                .rowsUpdated()
                .flatMap(rows ->
                        rows == 1 ? Mono.empty() : Mono.error(new IllegalStateException("principal insert failed")));
        DatabaseClient.GenericExecuteSpec serviceStatement = databaseClient
                .sql(
                        "INSERT INTO " + TABLE
                                + " (id,principal_id,tenant_id,service_account_name,owner_principal_id,purpose,expire_time,last_used_time,credential_policy_ext,enable_flag,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                                + " VALUES (:id,:principal_id,:tenant_id,:service_account_name,:owner_principal_id,:purpose,:expire_time,:last_used_time,"
                                + dialect.jsonWriteExpression(":credential_policy_ext")
                                + ",:enable_flag,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", serviceAccountId)
                .bind("principal_id", principalId)
                .bind("tenant_id", account.getTenantId())
                .bind("service_account_name", name)
                .bind("owner_principal_id", account.getOwnerPrincipalId())
                .bind("purpose", text(account.getPurpose()))
                .bind("credential_policy_ext", json(account.getCredentialPolicyExt()))
                .bind("enable_flag", enableFlag)
                .bind("remark", text(account.getRemark()))
                .bind("creator_id", value(account.getCreatorId()))
                .bind("creator_name", text(account.getCreatorName()))
                .bind("create_time", now)
                .bind("operator_id", value(account.getOperatorId()))
                .bind("operator_name", text(account.getOperatorName()))
                .bind("operate_time", now);
        serviceStatement = bindTime(serviceStatement, "expire_time", account.getExpireTime());
        serviceStatement = bindTime(serviceStatement, "last_used_time", account.getLastUsedTime());
        Mono<Void> service = serviceStatement
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? Mono.empty()
                        : Mono.error(new IllegalStateException("service account insert failed")));
        Mono<Void> membership = databaseClient
                .sql(
                        "INSERT INTO " + MEMBERSHIP
                                + " (id,tenant_id,principal_id,principal_type,membership_status,joined_time,membership_ext,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                                + " VALUES (:id,:tenant_id,:principal_id,'SERVICE_ACCOUNT','ACTIVE',:joined_time,"
                                + dialect.jsonWriteExpression(":membership_ext")
                                + ",:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", membershipId)
                .bind("tenant_id", account.getTenantId())
                .bind("principal_id", principalId)
                .bind("joined_time", now)
                .bind("membership_ext", "{}")
                .bind("remark", text(account.getRemark()))
                .bind("creator_id", value(account.getCreatorId()))
                .bind("creator_name", text(account.getCreatorName()))
                .bind("create_time", now)
                .bind("operator_id", value(account.getOperatorId()))
                .bind("operator_name", text(account.getOperatorName()))
                .bind("operate_time", now)
                .fetch()
                .rowsUpdated()
                .flatMap(rows ->
                        rows == 1 ? Mono.empty() : Mono.error(new IllegalStateException("membership insert failed")));
        return transactionalOperator
                .transactional(principal.then(service).then(membership))
                .then(getById(account.getTenantId(), serviceAccountId));
    }

    @Override
    public Mono<ServiceAccountDO> update(Long tenantId, ServiceAccountBO account) {
        if (!valid(tenantId, account == null ? null : account.getId())) return Mono.empty();
        byte enableFlag = flag(account.getEnableFlag());
        DatabaseClient.GenericExecuteSpec serviceStatement = databaseClient
                .sql("UPDATE " + TABLE
                        + " SET service_account_name=:service_account_name,owner_principal_id=:owner_principal_id,purpose=:purpose,expire_time=:expire_time,credential_policy_ext="
                        + dialect.jsonWriteExpression(":credential_policy_ext")
                        + ",enable_flag=:enable_flag,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0")
                .bind("service_account_name", text(account.getServiceAccountName()))
                .bind("owner_principal_id", account.getOwnerPrincipalId())
                .bind("purpose", text(account.getPurpose()))
                .bind("credential_policy_ext", json(account.getCredentialPolicyExt()))
                .bind("enable_flag", enableFlag)
                .bind("remark", text(account.getRemark()))
                .bind("operator_id", value(account.getOperatorId()))
                .bind("operator_name", text(account.getOperatorName()))
                .bind("operate_time", now())
                .bind("tenant_id", tenantId)
                .bind("id", account.getId());
        serviceStatement = bindTime(serviceStatement, "expire_time", account.getExpireTime());
        Mono<Void> service = serviceStatement
                .fetch()
                .rowsUpdated()
                .flatMap(rows ->
                        rows == 1 ? Mono.empty() : Mono.error(new IllegalStateException("service account not found")));
        Mono<Void> principal = databaseClient
                .sql("UPDATE " + PRINCIPAL
                        + " p SET principal_name=:principal_name,display_name=:display_name,enable_flag=:enable_flag,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                        + " FROM " + TABLE
                        + " a WHERE a.tenant_id=:tenant_id AND a.id=:id AND a.principal_id=p.id AND p.deleted=0")
                .bind("principal_name", tenantId + ":" + text(account.getServiceAccountName()))
                .bind("display_name", text(account.getServiceAccountName()))
                .bind("enable_flag", enableFlag)
                .bind("operator_id", value(account.getOperatorId()))
                .bind("operator_name", text(account.getOperatorName()))
                .bind("operate_time", now())
                .bind("tenant_id", tenantId)
                .bind("id", account.getId())
                .fetch()
                .rowsUpdated()
                .flatMap(rows ->
                        rows == 1 ? Mono.empty() : Mono.error(new IllegalStateException("principal not found")));
        return transactionalOperator.transactional(service.then(principal)).then(getById(tenantId, account.getId()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (!valid(tenantId, id)) return Mono.just(false);
        LocalDateTime now = now();
        DatabaseClient.GenericExecuteSpec account = databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .bind("operator_id", value(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", now);
        Mono<Long> deleted = account.fetch().rowsUpdated();
        Mono<Void> cascade = databaseClient
                .sql(
                        "UPDATE " + MEMBERSHIP
                                + " m SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time FROM "
                                + TABLE
                                + " a WHERE a.tenant_id=:tenant_id AND a.id=:id AND a.principal_id=m.principal_id AND m.tenant_id=:tenant_id AND m.deleted=0")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .bind("operator_id", value(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", now)
                .fetch()
                .rowsUpdated()
                .then()
                .then(databaseClient
                        .sql("UPDATE " + PRINCIPAL
                                + " p SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time FROM "
                                + TABLE
                                + " a WHERE a.tenant_id=:tenant_id AND a.id=:id AND a.principal_id=p.id AND p.deleted=0 AND NOT EXISTS (SELECT 1 FROM "
                                + MEMBERSHIP
                                + " m WHERE m.principal_id=p.id AND m.membership_status='ACTIVE' AND m.deleted=0)")
                        .bind("tenant_id", tenantId)
                        .bind("id", id)
                        .bind("operator_id", value(operatorId))
                        .bind("operator_name", text(operatorName))
                        .bind("operate_time", now)
                        .fetch()
                        .rowsUpdated()
                        .then());
        return transactionalOperator.transactional(
                deleted.flatMap(rows -> rows == 1 ? cascade.thenReturn(true) : Mono.just(false)));
    }

    private DatabaseClient.GenericExecuteSpec bind(
            DatabaseClient.GenericExecuteSpec spec, ServiceAccountFilter filter) {
        spec = spec.bind("tenant_id", filter.tenantId());
        if (filter.principalId() != null) spec = spec.bind("principal_id", filter.principalId());
        if (filter.serviceAccountName() != null)
            spec = spec.bind("service_account_name", "%" + filter.serviceAccountName() + "%");
        if (filter.ownerPrincipalId() != null) spec = spec.bind("owner_principal_id", filter.ownerPrincipalId());
        if (filter.enableFlag() != null)
            spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        return spec;
    }

    private ServiceAccountDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        ServiceAccountDO value = new ServiceAccountDO();
        value.setId(row.get("id", Long.class));
        value.setPrincipalId(row.get("principal_id", Long.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setServiceAccountName(row.get("service_account_name", String.class));
        value.setOwnerPrincipalId(row.get("owner_principal_id", Long.class));
        value.setPurpose(row.get("purpose", String.class));
        value.setExpireTime(time(row.get("expire_time")));
        value.setLastUsedTime(time(row.get("last_used_time")));
        value.setCredentialPolicyExt(jsonObject(row.get("credential_policy_ext", String.class)));
        Number flag = row.get("enable_flag", Number.class);
        value.setEnableFlag(flag == null ? null : flag.byteValue());
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

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "service_account_name ASC,id ASC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column =
                    switch (spec.field()) {
                        case "id" -> "id";
                        case "serviceAccountName", "service_account_name" -> "service_account_name";
                        case "ownerPrincipalId", "owner_principal_id" -> "owner_principal_id";
                        case "expireTime", "expire_time" -> "expire_time";
                        case "lastUsedTime", "last_used_time" -> "last_used_time";
                        case "createTime", "create_time" -> "create_time";
                        case "operateTime", "operate_time" -> "operate_time";
                        default ->
                            throw new IllegalArgumentException(
                                    "unsupported service account sort field: " + spec.field());
                    };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("id "))) clauses.add("id ASC");
        return String.join(",", clauses);
    }

    private DatabaseClient.GenericExecuteSpec bindTime(
            DatabaseClient.GenericExecuteSpec spec, String name, LocalDateTime value) {
        return value == null ? spec.bindNull(name, LocalDateTime.class) : spec.bind(name, value);
    }

    private JsonExt jsonObject(String raw) {
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

    private String json(JsonExt value) {
        return value == null ? "{}" : JsonUtil.toJsonString(value);
    }

    private long id() {
        return UuidV7.nextLong();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private byte flag(EnableFlagEnum value) {
        return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private boolean valid(Long tenantId, Long id) {
        return tenantId != null && tenantId > 0 && id != null && id > 0;
    }
}
