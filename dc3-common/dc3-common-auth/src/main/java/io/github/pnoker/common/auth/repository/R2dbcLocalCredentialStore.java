package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.LocalCredentialDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Explicit SQL adapter for local credentials joined through active memberships. */
@Repository
@ConditionalOnClass({DatabaseClient.class, PageTransaction.class})
@RequiredArgsConstructor
public class R2dbcLocalCredentialStore implements ReactiveLocalCredentialStore {

    private static final String TABLE = "dc3_auth.dc3_local_credential";
    private static final String MEMBERSHIP = "dc3_auth.dc3_tenant_membership";
    private static final String COLUMNS = "c.id,c.principal_id,c.login_name,c.login_name_normalized,c.credential_type,"
            + "c.password_hash,c.password_algorithm,c.password_params,c.password_updated_time,c.password_expire_time,"
            + "c.failed_attempts,c.locked_until,c.require_password_change,c.enable_flag,c.credential_ext,c.remark,"
            + "c.creator_id,c.creator_name,c.create_time,c.operator_id,c.operator_name,c.operate_time,c.deleted";

    private final DatabaseClient databaseClient;
    private final PageTransaction pageTransaction;

    @Override
    public Mono<LocalCredentialDO> getById(Long tenantId, Long id) {
        if (!valid(tenantId, id)) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " c WHERE c.id=:id AND c.deleted=0"
                        + " AND EXISTS (SELECT 1 FROM " + MEMBERSHIP + " m WHERE m.tenant_id=:tenant_id"
                        + " AND m.principal_id=c.principal_id AND m.membership_status='ACTIVE' AND m.deleted=0) LIMIT 1")
                .bind("tenant_id", tenantId).bind("id", id).map(this::map).one();
    }

    @Override
    public Mono<LocalCredentialDO> getByLoginName(Long tenantId, String loginNameNormalized) {
        if (!valid(tenantId) || loginNameNormalized == null || loginNameNormalized.isBlank()) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " c WHERE c.login_name_normalized=:login_name"
                        + " AND c.credential_type='PASSWORD' AND c.enable_flag=0 AND c.deleted=0"
                        + " AND EXISTS (SELECT 1 FROM " + MEMBERSHIP + " m WHERE m.tenant_id=:tenant_id"
                        + " AND m.principal_id=c.principal_id AND m.membership_status='ACTIVE' AND m.deleted=0) LIMIT 1")
                .bind("tenant_id", tenantId).bind("login_name", loginNameNormalized).map(this::map).one();
    }

    @Override
    public Mono<Boolean> existsByLoginName(Long tenantId, String loginNameNormalized) {
        if (!valid(tenantId) || loginNameNormalized == null || loginNameNormalized.isBlank()) return Mono.just(false);
        return databaseClient.sql("SELECT 1 FROM " + TABLE + " c WHERE c.login_name_normalized=:login_name"
                        + " AND c.credential_type='PASSWORD' AND c.deleted=0"
                        + " AND EXISTS (SELECT 1 FROM " + MEMBERSHIP + " m WHERE m.tenant_id=:tenant_id"
                        + " AND m.principal_id=c.principal_id AND m.membership_status='ACTIVE' AND m.deleted=0) LIMIT 1")
                .bind("tenant_id", tenantId).bind("login_name", loginNameNormalized)
                .map((row, metadata) -> Boolean.TRUE).one().defaultIfEmpty(false);
    }

    @Override
    public Mono<OffsetPage<LocalCredentialDO>> list(LocalCredentialFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("local credential filter is required"));
        StringBuilder where = new StringBuilder(" WHERE c.deleted=0 AND EXISTS (SELECT 1 FROM ")
                .append(MEMBERSHIP).append(" m WHERE m.tenant_id=:tenant_id AND m.principal_id=c.principal_id")
                .append(" AND m.membership_status='ACTIVE' AND m.deleted=0)");
        if (filter.principalId() != null) where.append(" AND c.principal_id=:principal_id");
        if (filter.loginName() != null) where.append(" AND c.login_name_normalized LIKE :login_name");
        if (filter.credentialType() != null) where.append(" AND c.credential_type=:credential_type");
        if (filter.enableFlag() != null) where.append(" AND c.enable_flag=:enable_flag");
        String condition = where.toString();
        DatabaseClient.GenericExecuteSpec count = databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + " c" + condition);
        DatabaseClient.GenericExecuteSpec rows = databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " c" + condition
                        + " ORDER BY " + orderBy(filter.page().sort()) + " LIMIT :limit OFFSET :offset")
                .bind("limit", filter.page().limit()).bind("offset", filter.page().offset());
        count = bind(count, filter);
        rows = bind(rows, filter);
        Mono<Long> total = count.map((row, metadata) -> {
            Number value = row.get("total", Number.class);
            return value == null ? 0L : value.longValue();
        }).one().defaultIfEmpty(0L);
        DatabaseClient.GenericExecuteSpec itemRows = rows;
        return total.flatMap(totalCount -> itemRows.map(this::map).all().collectList()
                        .map(items -> OffsetPage.of(items, filter.page().offset(), filter.page().limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec query, LocalCredentialFilter filter) {
        query = query.bind("tenant_id", filter.tenantId());
        if (filter.principalId() != null) query = query.bind("principal_id", filter.principalId());
        if (filter.loginName() != null) query = query.bind("login_name", "%" + filter.loginName() + "%");
        if (filter.credentialType() != null) query = query.bind("credential_type", filter.credentialType().getValue());
        if (filter.enableFlag() != null) query = query.bind("enable_flag", filter.enableFlag().getIndex());
        return query;
    }

    private LocalCredentialDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        LocalCredentialDO value = new LocalCredentialDO();
        value.setId(row.get("id", Long.class));
        value.setPrincipalId(row.get("principal_id", Long.class));
        value.setLoginName(row.get("login_name", String.class));
        value.setLoginNameNormalized(row.get("login_name_normalized", String.class));
        value.setCredentialType(row.get("credential_type", String.class));
        value.setPasswordHash(row.get("password_hash", String.class));
        value.setPasswordAlgorithm(row.get("password_algorithm", String.class));
        value.setPasswordParams(json(row.get("password_params", String.class)));
        value.setPasswordUpdatedTime(time(row.get("password_updated_time")));
        value.setPasswordExpireTime(time(row.get("password_expire_time")));
        Number failed = row.get("failed_attempts", Number.class);
        value.setFailedAttempts(failed == null ? null : failed.intValue());
        value.setLockedUntil(time(row.get("locked_until")));
        Number requireChange = row.get("require_password_change", Number.class);
        value.setRequirePasswordChange(requireChange == null ? null : requireChange.byteValue());
        Number enable = row.get("enable_flag", Number.class);
        value.setEnableFlag(enable == null ? null : enable.byteValue());
        value.setCredentialExt(json(row.get("credential_ext", String.class)));
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
        try { return JsonUtil.parseObject(raw, JsonExt.class); } catch (RuntimeException ignored) { return null; }
    }

    private LocalDateTime time(Object raw) {
        if (raw instanceof LocalDateTime value) return value;
        if (raw instanceof OffsetDateTime value) return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (raw instanceof Instant value) return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
        return null;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "c.login_name ASC,c.id ASC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column = switch (spec.field()) {
                case "id" -> "c.id";
                case "loginName" -> "c.login_name";
                case "credentialType" -> "c.credential_type";
                case "enableFlag" -> "c.enable_flag";
                case "passwordUpdatedTime" -> "c.password_updated_time";
                case "createTime" -> "c.create_time";
                case "operateTime" -> "c.operate_time";
                default -> throw new IllegalArgumentException("unsupported local credential sort field: " + spec.field());
            };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("c.id "))) clauses.add("c.id ASC");
        return String.join(",", clauses);
    }

    private boolean valid(Long tenantId) { return tenantId != null && tenantId > 0; }
    private boolean valid(Long tenantId, Long id) { return valid(tenantId) && id != null && id > 0; }
}
