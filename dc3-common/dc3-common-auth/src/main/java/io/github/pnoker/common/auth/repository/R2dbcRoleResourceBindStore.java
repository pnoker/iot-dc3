package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.model.RoleResourceBindDO;
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

/** Explicit SQL adapter for role-resource bindings with tenant ownership joins. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcRoleResourceBindStore implements ReactiveRoleResourceBindStore {
    private static final String TABLE = "dc3_auth.dc3_role_resource_bind";
    private static final String ROLE = "dc3_auth.dc3_role";
    private static final String PRINCIPAL_BIND = "dc3_auth.dc3_role_principal_bind";
    private static final String COLUMNS = "rr.id,rr.role_id,rr.resource_id,rr.remark,rr.creator_id,rr.creator_name,rr.create_time,rr.operator_id,rr.operator_name,rr.operate_time,rr.deleted";
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;

    @Override
    public Mono<OffsetPage<RoleResourceBindDO>> list(RoleResourceBindFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("role resource bind filter is required"));
        StringBuilder where = new StringBuilder(" FROM ").append(TABLE).append(" rr JOIN ").append(ROLE)
                .append(" r ON r.id=rr.role_id AND r.tenant_id=:tenant_id AND r.deleted=0 WHERE rr.deleted=0");
        if (filter.roleId() != null) where.append(" AND rr.role_id=:role_id");
        if (filter.resourceId() != null) where.append(" AND rr.resource_id=:resource_id");
        DatabaseClient.GenericExecuteSpec count = bind(databaseClient.sql("SELECT COUNT(*) AS total" + where), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(databaseClient.sql("SELECT " + COLUMNS + where
                        + " ORDER BY " + orderBy(filter.page().sort()) + " LIMIT :limit OFFSET :offset"), filter)
                .bind("limit", filter.page().limit()).bind("offset", filter.page().offset());
        Mono<Long> total = count.map((row, metadata) -> Optional.ofNullable(row.get("total", Number.class)).map(Number::longValue).orElse(0L)).one().defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(this::map).all().collectList()
                        .map(items -> OffsetPage.of(items, filter.page().offset(), filter.page().limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<RoleResourceBindDO> insert(RoleResourceBindBO binding) {
        if (binding == null || !valid(binding.getRoleId()) || !valid(binding.getResourceId())) return Mono.error(new IllegalArgumentException("binding identifiers are required"));
        long id = id(); LocalDateTime now = now();
        String sql = "INSERT INTO " + TABLE + " (id,role_id,resource_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:role_id,:resource_id,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        return transactionalOperator.transactional(databaseClient.sql(sql).bind("id", id).bind("role_id", binding.getRoleId()).bind("resource_id", binding.getResourceId()).bind("remark", text(binding.getRemark())).bind("creator_id", value(binding.getCreatorId())).bind("creator_name", text(binding.getCreatorName())).bind("create_time", now).bind("operator_id", value(binding.getOperatorId())).bind("operator_name", text(binding.getOperatorName())).bind("operate_time", now).fetch().rowsUpdated()).then(getById(binding.getRoleId(), id));
    }

    private Mono<RoleResourceBindDO> getById(Long roleId, Long id) {
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " rr WHERE rr.role_id=:role_id AND rr.id=:id AND rr.deleted=0 LIMIT 1").bind("role_id", roleId).bind("id", id).map(this::map).one();
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (!valid(tenantId) || !valid(id)) return Mono.just(false);
        String sql = "UPDATE " + TABLE + " rr SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE rr.id=:id AND rr.deleted=0 AND EXISTS (SELECT 1 FROM " + ROLE + " r WHERE r.id=rr.role_id AND r.tenant_id=:tenant_id AND r.deleted=0)";
        return transactionalOperator.transactional(databaseClient.sql(sql).bind("tenant_id", tenantId).bind("id", id).bind("operator_id", value(operatorId)).bind("operator_name", text(operatorName)).bind("operate_time", now()).fetch().rowsUpdated()).map(rows -> rows == 1);
    }

    @Override
    public Mono<Boolean> exists(Long tenantId, Long roleId, Long resourceId) {
        if (!valid(tenantId) || !valid(roleId) || !valid(resourceId)) return Mono.just(false);
        return databaseClient.sql("SELECT 1 FROM " + TABLE + " rr JOIN " + ROLE + " r ON r.id=rr.role_id AND r.tenant_id=:tenant_id AND r.deleted=0 WHERE rr.role_id=:role_id AND rr.resource_id=:resource_id AND rr.deleted=0 LIMIT 1").bind("tenant_id", tenantId).bind("role_id", roleId).bind("resource_id", resourceId).fetch().first().hasElement();
    }

    @Override public Flux<Long> listResourceIds(Long tenantId, Long roleId) { if (!valid(tenantId) || !valid(roleId)) return Flux.empty(); return databaseClient.sql("SELECT rr.resource_id FROM " + TABLE + " rr JOIN " + ROLE + " r ON r.id=rr.role_id AND r.tenant_id=:tenant_id AND r.deleted=0 WHERE rr.role_id=:role_id AND rr.deleted=0 ORDER BY rr.resource_id").bind("tenant_id", tenantId).bind("role_id", roleId).map((row,m)->row.get("resource_id",Long.class)).all(); }
    @Override public Flux<Long> listResourceIdsByPrincipal(Long tenantId, Long principalId) { if (!valid(tenantId) || !valid(principalId)) return Flux.empty(); return databaseClient.sql("SELECT DISTINCT rr.resource_id FROM " + PRINCIPAL_BIND + " rp JOIN " + ROLE + " r ON r.id=rp.role_id AND r.tenant_id=rp.tenant_id AND r.deleted=0 AND r.enable_flag=0 JOIN " + TABLE + " rr ON rr.role_id=r.id AND rr.deleted=0 WHERE rp.tenant_id=:tenant_id AND rp.principal_id=:principal_id AND rp.deleted=0 ORDER BY rr.resource_id").bind("tenant_id", tenantId).bind("principal_id", principalId).map((row,m)->row.get("resource_id",Long.class)).all(); }
    @Override public Flux<Long> listRoleIdsByResource(Long tenantId, Long resourceId) { if (!valid(tenantId) || !valid(resourceId)) return Flux.empty(); return databaseClient.sql("SELECT rr.role_id FROM " + TABLE + " rr JOIN " + ROLE + " r ON r.id=rr.role_id AND r.tenant_id=:tenant_id AND r.deleted=0 AND r.enable_flag=0 WHERE rr.resource_id=:resource_id AND rr.deleted=0 ORDER BY rr.role_id").bind("tenant_id", tenantId).bind("resource_id", resourceId).map((row,m)->row.get("role_id",Long.class)).all(); }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec query, RoleResourceBindFilter filter) { query = query.bind("tenant_id", filter.tenantId()); if (filter.roleId() != null) query = query.bind("role_id", filter.roleId()); if (filter.resourceId() != null) query = query.bind("resource_id", filter.resourceId()); return query; }
    private RoleResourceBindDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) { RoleResourceBindDO value = new RoleResourceBindDO(); value.setId(row.get("id",Long.class)); value.setRoleId(row.get("role_id",Long.class)); value.setResourceId(row.get("resource_id",Long.class)); value.setRemark(row.get("remark",String.class)); value.setCreatorId(row.get("creator_id",Long.class)); value.setCreatorName(row.get("creator_name",String.class)); value.setCreateTime(time(row.get("create_time"))); value.setOperatorId(row.get("operator_id",Long.class)); value.setOperatorName(row.get("operator_name",String.class)); value.setOperateTime(time(row.get("operate_time"))); Number deleted=row.get("deleted",Number.class); value.setDeleted(deleted==null?null:deleted.byteValue()); return value; }
    private String orderBy(List<SortSpec> sort) { if (sort==null||sort.isEmpty()) return "rr.role_id ASC,rr.resource_id ASC,rr.id ASC"; List<String> clauses=new ArrayList<>(); for (SortSpec spec:sort) { String column=switch(spec.field()){case "id"->"rr.id";case "roleId"->"rr.role_id";case "resourceId"->"rr.resource_id";case "createTime"->"rr.create_time";case "operateTime"->"rr.operate_time";default->throw new IllegalArgumentException("unsupported role resource bind sort field: "+spec.field());}; clauses.add(column+" "+spec.direction().name()); } if(clauses.stream().noneMatch(v->v.startsWith("rr.id ")))clauses.add("rr.id ASC"); return String.join(",",clauses); }
    private String text(String value){return value==null?"":value;} private long value(Long value){return value==null?0L:value;} private long id(){return UuidV7.nextLong();} private LocalDateTime now(){return LocalDateTime.now(ZoneOffset.UTC);} private LocalDateTime time(Object raw){if(raw instanceof LocalDateTime value)return value;if(raw instanceof OffsetDateTime value)return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();if(raw instanceof Instant value)return LocalDateTime.ofInstant(value,ZoneOffset.UTC);return null;} private boolean valid(Long value){return value!=null&&value>0;}
}
