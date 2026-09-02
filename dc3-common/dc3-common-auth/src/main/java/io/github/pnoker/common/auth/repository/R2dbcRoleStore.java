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

import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.model.RoleDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.RoleExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcRoleStore implements ReactiveRoleStore {
    private static final String TABLE = "dc3_auth.dc3_role";
    private static final String COLUMNS =
            "id,parent_role_id,role_name,role_code,role_ext,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted";
    private final DatabaseClient db;
    private final TransactionalOperator tx;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<RoleDO> getById(Long tenantId, Long id) {
        if (!valid(tenantId, id)) return Mono.empty();
        return db.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<OffsetPage<RoleDO>> list(RoleFilter f) {
        if (f == null) return Mono.error(new IllegalArgumentException("role filter is required"));
        StringBuilder w = new StringBuilder(" FROM " + TABLE + " WHERE tenant_id=:tenant_id AND deleted=0");
        if (f.roleName() != null) w.append(" AND role_name LIKE :role_name");
        if (f.roleCode() != null) w.append(" AND role_code=:role_code");
        if (f.enableFlag() != null) w.append(" AND enable_flag=:enable_flag");
        DatabaseClient.GenericExecuteSpec c = bind(db.sql("SELECT COUNT(*) AS total" + w), f);
        DatabaseClient.GenericExecuteSpec r = bind(
                        db.sql("SELECT " + COLUMNS + w + " ORDER BY "
                                + order(f.page().sort()) + " LIMIT :limit OFFSET :offset"),
                        f)
                .bind("limit", f.page().limit())
                .bind("offset", f.page().offset());
        Mono<Long> total = c.map((row, m) -> Optional.ofNullable(row.get("total", Number.class))
                        .map(Number::longValue)
                        .orElse(0L))
                .one()
                .defaultIfEmpty(0L);
        return total.flatMap(totalCount -> r.map(this::map)
                        .all()
                        .collectList()
                        .map(items ->
                                OffsetPage.of(items, f.page().offset(), f.page().limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Flux<RoleDO> listTree(RoleFilter f) {
        if (f == null) return Flux.error(new IllegalArgumentException("role filter is required"));
        StringBuilder w = new StringBuilder(" FROM " + TABLE + " WHERE tenant_id=:tenant_id AND deleted=0");
        if (f.roleName() != null) w.append(" AND role_name LIKE :role_name");
        if (f.roleCode() != null) w.append(" AND role_code=:role_code");
        if (f.enableFlag() != null) w.append(" AND enable_flag=:enable_flag");
        return bind(db.sql("SELECT " + COLUMNS + w + " ORDER BY role_name ASC,id ASC"), f)
                .map(this::map)
                .all();
    }

    @Override
    public Mono<RoleDO> insert(RoleBO b) {
        if (b == null || b.getTenantId() == null || b.getTenantId() <= 0)
            return Mono.error(new IllegalArgumentException("tenant id is required"));
        long id = id();
        LocalDateTime now = now();
        return tx.transactional(db.sql(
                                "INSERT INTO " + TABLE
                                        + " (id,parent_role_id,role_name,role_code,role_ext,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:parent_role_id,:role_name,:role_code,"
                                        + dialect.jsonWriteExpression(":role_ext")
                                        + ",:enable_flag,:tenant_id,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                        .bind("id", id)
                        .bind("parent_role_id", parseId(b.getParentRoleId()))
                        .bind("role_name", text(b.getRoleName()))
                        .bind("role_code", text(b.getRoleCode()))
                        .bind("role_ext", roleJson(b.getRoleExt()))
                        .bind("enable_flag", flag(b.getEnableFlag()))
                        .bind("tenant_id", b.getTenantId())
                        .bind("remark", text(b.getRemark()))
                        .bind("creator_id", value(b.getCreatorId()))
                        .bind("creator_name", text(b.getCreatorName()))
                        .bind("create_time", now)
                        .bind("operator_id", value(b.getOperatorId()))
                        .bind("operator_name", text(b.getOperatorName()))
                        .bind("operate_time", now)
                        .fetch()
                        .rowsUpdated())
                .then(getById(b.getTenantId(), id));
    }

    @Override
    public Mono<RoleDO> update(Long tenantId, RoleBO b) {
        if (!valid(tenantId, b == null ? null : b.getId())) return Mono.empty();
        return tx.transactional(db.sql(
                                "UPDATE " + TABLE
                                        + " SET parent_role_id=:parent_role_id,role_name=:role_name,role_code=:role_code,role_ext="
                                        + dialect.jsonWriteExpression(":role_ext")
                                        + ",enable_flag=:enable_flag,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0")
                        .bind("parent_role_id", parseId(b.getParentRoleId()))
                        .bind("role_name", text(b.getRoleName()))
                        .bind("role_code", text(b.getRoleCode()))
                        .bind("role_ext", roleJson(b.getRoleExt()))
                        .bind("enable_flag", flag(b.getEnableFlag()))
                        .bind("remark", text(b.getRemark()))
                        .bind("operator_id", value(b.getOperatorId()))
                        .bind("operator_name", text(b.getOperatorName()))
                        .bind("operate_time", now())
                        .bind("tenant_id", tenantId)
                        .bind("id", b.getId())
                        .fetch()
                        .rowsUpdated())
                .flatMap(n -> n == 1 ? getById(tenantId, b.getId()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long op, String name) {
        if (!valid(tenantId, id)) return Mono.just(false);
        return tx.transactional(db.sql("UPDATE " + TABLE
                        + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 AND NOT EXISTS (SELECT 1 FROM "
                        + TABLE + " c WHERE c.tenant_id=:tenant_id AND c.parent_role_id=:id AND c.deleted=0)")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .bind("operator_id", value(op))
                .bind("operator_name", text(name))
                .bind("operate_time", now())
                .fetch()
                .rowsUpdated()
                .map(n -> n == 1));
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec s, RoleFilter f) {
        s = s.bind("tenant_id", f.tenantId());
        if (f.roleName() != null) s = s.bind("role_name", "%" + f.roleName() + "%");
        if (f.roleCode() != null) s = s.bind("role_code", f.roleCode());
        if (f.enableFlag() != null) s = s.bind("enable_flag", f.enableFlag().getIndex());
        return s;
    }

    private RoleDO map(io.r2dbc.spi.Row r, io.r2dbc.spi.RowMetadata m) {
        RoleDO d = new RoleDO();
        d.setId(r.get("id", Long.class));
        Number p = r.get("parent_role_id", Number.class);
        d.setParentRoleId(p == null ? null : p.longValue());
        d.setRoleName(r.get("role_name", String.class));
        d.setRoleCode(r.get("role_code", String.class));
        d.setRoleExt(json(r.get("role_ext", String.class)));
        Number e = r.get("enable_flag", Number.class);
        d.setEnableFlag(e == null ? null : e.byteValue());
        d.setTenantId(r.get("tenant_id", Long.class));
        d.setRemark(r.get("remark", String.class));
        d.setCreatorId(r.get("creator_id", Long.class));
        d.setCreatorName(r.get("creator_name", String.class));
        d.setCreateTime(time(r.get("create_time")));
        d.setOperatorId(r.get("operator_id", Long.class));
        d.setOperatorName(r.get("operator_name", String.class));
        d.setOperateTime(time(r.get("operate_time")));
        Number x = r.get("deleted", Number.class);
        d.setDeleted(x == null ? null : x.byteValue());
        return d;
    }

    private String order(List<SortSpec> s) {
        if (s == null || s.isEmpty()) return "role_name ASC,id ASC";
        List<String> o = new ArrayList<>();
        for (SortSpec x : s) {
            String c =
                    switch (x.field()) {
                        case "id" -> "id";
                        case "roleName" -> "role_name";
                        case "roleCode" -> "role_code";
                        case "createTime" -> "create_time";
                        case "operateTime" -> "operate_time";
                        default -> throw new IllegalArgumentException("unsupported role sort field: " + x.field());
                    };
            o.add(c + " " + x.direction().name());
        }
        if (o.stream().noneMatch(v -> v.startsWith("id "))) o.add("id ASC");
        return String.join(",", o);
    }

    private LocalDateTime time(Object x) {
        if (x instanceof LocalDateTime v) return v;
        if (x instanceof OffsetDateTime v)
            return v.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (x instanceof Instant v) return LocalDateTime.ofInstant(v, ZoneOffset.UTC);
        return null;
    }

    private JsonExt json(String x) {
        if (x == null) return null;
        try {
            return JsonUtil.parseObject(x, JsonExt.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String text(String x) {
        return x == null ? "" : x;
    }

    private long value(Long x) {
        return x == null ? 0 : x;
    }

    private byte flag(EnableFlagEnum x) {
        return x == null ? EnableFlagEnum.ENABLE.getIndex() : x.getIndex();
    }

    private long id() {
        return UuidV7.nextLong();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private boolean valid(Long t, Long i) {
        return t != null && t > 0 && i != null && i > 0;
    }

    private Long parseId(String x) {
        if (x == null || x.isBlank() || "0".equals(x)) return 0L;
        try {
            return Long.valueOf(x);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("parent role id is invalid", e);
        }
    }

    private String roleJson(RoleExt x) {
        if (x == null) return "{}";
        JsonExt j = new JsonExt();
        j.setType(x.getType());
        j.setVersion(x.getVersion());
        j.setRemark(x.getRemark());
        j.setContent(JsonUtil.toJsonString(x.getContent()));
        return JsonUtil.toJsonString(j);
    }
}
