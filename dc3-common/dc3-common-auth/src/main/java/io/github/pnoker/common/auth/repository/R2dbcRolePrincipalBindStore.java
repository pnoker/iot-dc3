package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.model.RolePrincipalBindDO;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Explicit SQL adapter for tenant-scoped role-principal bindings. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcRolePrincipalBindStore implements ReactiveRolePrincipalBindStore {
    private static final String TABLE = "dc3_auth.dc3_role_principal_bind";
    private static final String COLUMNS = "id,tenant_id,role_id,principal_id,principal_type,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;

    @Override
    public Mono<RolePrincipalBindDO> getById(Long tenantId, Long id) {
        if (!valid(tenantId, id)) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId).bind("id", id).map(this::map).one();
    }

    @Override
    public Mono<OffsetPage<RolePrincipalBindDO>> list(RolePrincipalBindFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("role principal bind filter is required"));
        StringBuilder where = new StringBuilder(" FROM ").append(TABLE)
                .append(" WHERE tenant_id=:tenant_id AND deleted=0");
        if (filter.roleId() != null) where.append(" AND role_id=:role_id");
        if (filter.principalId() != null) where.append(" AND principal_id=:principal_id");
        if (filter.principalType() != null) where.append(" AND principal_type=:principal_type");
        DatabaseClient.GenericExecuteSpec count = bind(databaseClient.sql("SELECT COUNT(*) AS total" + where), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(databaseClient.sql("SELECT " + COLUMNS + where
                        + " ORDER BY " + orderBy(filter.page().sort()) + " LIMIT :limit OFFSET :offset"), filter)
                .bind("limit", filter.page().limit()).bind("offset", filter.page().offset());
        Mono<Long> total = count.map((row, metadata) -> Optional.ofNullable(row.get("total", Number.class))
                        .map(Number::longValue).orElse(0L)).one().defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(this::map).all().collectList()
                        .map(items -> OffsetPage.of(items, filter.page().offset(), filter.page().limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<RolePrincipalBindDO> insert(RolePrincipalBindBO binding) {
        if (binding == null || !valid(binding.getTenantId(), binding.getRoleId())
                || !valid(binding.getPrincipalId())) return Mono.error(new IllegalArgumentException("binding identifiers are required"));
        long id = id();
        LocalDateTime now = now();
        String sql = "INSERT INTO " + TABLE + " (id,tenant_id,role_id,principal_id,principal_type,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                + " VALUES (:id,:tenant_id,:role_id,:principal_id,:principal_type,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        return transactionalOperator.transactional(databaseClient.sql(sql)
                        .bind("id", id).bind("tenant_id", binding.getTenantId()).bind("role_id", binding.getRoleId())
                        .bind("principal_id", binding.getPrincipalId()).bind("principal_type", principalType(binding.getPrincipalType()))
                        .bind("remark", text(binding.getRemark())).bind("creator_id", value(binding.getCreatorId()))
                        .bind("creator_name", text(binding.getCreatorName())).bind("create_time", now)
                        .bind("operator_id", value(binding.getOperatorId())).bind("operator_name", text(binding.getOperatorName()))
                        .bind("operate_time", now).fetch().rowsUpdated())
                .then(getById(binding.getTenantId(), id));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (!valid(tenantId, id)) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient.sql("UPDATE " + TABLE
                        + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0")
                .bind("tenant_id", tenantId).bind("id", id).bind("operator_id", value(operatorId))
                .bind("operator_name", text(operatorName)).bind("operate_time", now()).fetch().rowsUpdated())
                .map(rows -> rows == 1);
    }

    @Override
    public Mono<Boolean> exists(Long tenantId, Long roleId, Long principalId, Long excludedId) {
        if (!valid(tenantId, roleId) || !valid(principalId)) return Mono.just(false);
        StringBuilder sql = new StringBuilder("SELECT 1 FROM ").append(TABLE)
                .append(" WHERE tenant_id=:tenant_id AND role_id=:role_id AND principal_id=:principal_id AND deleted=0");
        if (excludedId != null) sql.append(" AND id<>:excluded_id");
        sql.append(" LIMIT 1");
        DatabaseClient.GenericExecuteSpec query = databaseClient.sql(sql.toString())
                .bind("tenant_id", tenantId).bind("role_id", roleId).bind("principal_id", principalId);
        if (excludedId != null) query = query.bind("excluded_id", excludedId);
        return query.fetch().first().hasElement();
    }

    @Override
    public Flux<Long> listRoleIds(Long tenantId, Long principalId) {
        if (!valid(tenantId, principalId)) return Flux.empty();
        return databaseClient.sql("SELECT role_id FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND principal_id=:principal_id AND deleted=0 ORDER BY role_id")
                .bind("tenant_id", tenantId).bind("principal_id", principalId)
                .map((row, metadata) -> row.get("role_id", Long.class)).all();
    }

    @Override
    public Flux<Long> listPrincipalIds(Long tenantId, Long roleId, String principalType) {
        if (!valid(tenantId, roleId)) return Flux.empty();
        StringBuilder sql = new StringBuilder("SELECT principal_id FROM ").append(TABLE)
                .append(" WHERE tenant_id=:tenant_id AND role_id=:role_id AND deleted=0");
        if (principalType != null) sql.append(" AND principal_type=:principal_type");
        sql.append(" ORDER BY principal_id");
        DatabaseClient.GenericExecuteSpec query = databaseClient.sql(sql.toString())
                .bind("tenant_id", tenantId).bind("role_id", roleId);
        if (principalType != null) query = query.bind("principal_type", principalType);
        return query.map((row, metadata) -> row.get("principal_id", Long.class)).all();
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec query, RolePrincipalBindFilter filter) {
        query = query.bind("tenant_id", filter.tenantId());
        if (filter.roleId() != null) query = query.bind("role_id", filter.roleId());
        if (filter.principalId() != null) query = query.bind("principal_id", filter.principalId());
        if (filter.principalType() != null) query = query.bind("principal_type", filter.principalType().getValue());
        return query;
    }

    private RolePrincipalBindDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        RolePrincipalBindDO value = new RolePrincipalBindDO();
        value.setId(row.get("id", Long.class)); value.setTenantId(row.get("tenant_id", Long.class));
        value.setRoleId(row.get("role_id", Long.class)); value.setPrincipalId(row.get("principal_id", Long.class));
        value.setPrincipalType(row.get("principal_type", String.class)); value.setRemark(row.get("remark", String.class));
        value.setCreatorId(row.get("creator_id", Long.class)); value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(time(row.get("create_time"))); value.setOperatorId(row.get("operator_id", Long.class));
        value.setOperatorName(row.get("operator_name", String.class)); value.setOperateTime(time(row.get("operate_time")));
        Number deleted = row.get("deleted", Number.class); value.setDeleted(deleted == null ? null : deleted.byteValue());
        return value;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "role_id ASC,principal_id ASC,id ASC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column = switch (spec.field()) {
                case "id" -> "id"; case "tenantId" -> "tenant_id"; case "roleId" -> "role_id";
                case "principalId" -> "principal_id"; case "principalType" -> "principal_type";
                case "createTime" -> "create_time"; case "operateTime" -> "operate_time";
                default -> throw new IllegalArgumentException("unsupported role principal bind sort field: " + spec.field());
            };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("id "))) clauses.add("id ASC");
        return String.join(",", clauses);
    }

    private String principalType(PrincipalTypeEnum value) { return value == null ? PrincipalTypeEnum.USER.getValue() : value.getValue(); }
    private String text(String value) { return value == null ? "" : value; }
    private long value(Long value) { return value == null ? 0L : value; }
    private long id() { return UuidV7.nextLong(); }
    private LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private LocalDateTime time(Object raw) {
        if (raw instanceof LocalDateTime value) return value;
        if (raw instanceof OffsetDateTime value) return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (raw instanceof Instant value) return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
        return null;
    }
    private boolean valid(Long value) { return value != null && value > 0; }
    private boolean valid(Long tenantId, Long id) { return valid(tenantId) && valid(id); }
}
