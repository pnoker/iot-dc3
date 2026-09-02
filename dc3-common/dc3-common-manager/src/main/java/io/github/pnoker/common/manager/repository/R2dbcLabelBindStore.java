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
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.manager.entity.bo.LabelBindBO;
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
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
@ConditionalOnClass({DatabaseClient.class, R2dbcDialect.class, TransactionalOperator.class})
public class R2dbcLabelBindStore implements ReactiveLabelBindStore {
    private static final String T = "dc3_manager.dc3_label_bind",
            C =
                    "id,entity_type_flag,label_id,entity_id,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time";
    private final DatabaseClient c;
    private final R2dbcDialect d;
    private final TransactionalOperator tx;
    private final PageTransaction pageTransaction;

    public Mono<OffsetPage<LabelBindBO>> list(BindingFilter f) {
        StringBuilder w = new StringBuilder(" WHERE tenant_id=:tenant AND deleted=0");
        if (f.entityType() != null) w.append(" AND entity_type_flag=:type");
        if (f.ownerId() != null) w.append(" AND label_id=:owner");
        if (f.entityId() != null) w.append(" AND entity_id=:entity");
        Mono<List<LabelBindBO>> i = bind(
                        c.sql("SELECT " + C + " FROM " + T + w + " ORDER BY " + order(f.sort())
                                + " LIMIT :limit OFFSET :offset"),
                        f)
                .bind("limit", f.limit())
                .bind("offset", f.offset())
                .map(this::map)
                .all()
                .collectList();
        Mono<Long> n = bind(c.sql("SELECT COUNT(*) total FROM " + T + w), f)
                .map((r, m) -> num(r.get("total")).longValue())
                .one()
                .defaultIfEmpty(0L);
        return n.flatMap(total -> i.map(items ->
                        new OffsetPage<>(items, f.offset(), f.limit(), total, f.offset() + items.size() < total)))
                .as(pageTransaction::transactional);
    }

    public Mono<LabelBindBO> get(Long t, Long id) {
        return !ok(t) || !ok(id)
                ? Mono.empty()
                : c.sql("SELECT " + C + " FROM " + T + " WHERE tenant_id=:tenant AND id=:id AND deleted=0 LIMIT 1")
                        .bind("tenant", t)
                        .bind("id", id)
                        .map(this::map)
                        .one();
    }

    public Mono<LabelBindBO> getByEntity(Long t, byte type, Long owner, Long entity) {
        return c.sql(
                        "SELECT " + C + " FROM " + T
                                + " WHERE tenant_id=:tenant AND entity_type_flag=:type AND label_id=:owner AND entity_id=:entity AND deleted=0 LIMIT 1")
                .bind("tenant", t)
                .bind("type", type)
                .bind("owner", owner)
                .bind("entity", entity)
                .map(this::map)
                .one();
    }

    public Mono<LabelBindBO> insert(LabelBindBO v) {
        v.setId(v.getId() == null ? UuidV7.nextLong() : v.getId());
        v.setCreateTime(now());
        v.setOperateTime(v.getCreateTime());
        String s = "INSERT INTO " + T
                + " (id,entity_type_flag,label_id,entity_id,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:type,:owner,:entity,:tenant,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        return tx.transactional(write(s, v, true)
                .fetch()
                .rowsUpdated()
                .flatMap(n -> n == 1 ? get(v.getTenantId(), v.getId()) : Mono.empty()));
    }

    public Mono<LabelBindBO> update(LabelBindBO v) {
        v.setOperateTime(now());
        String s = "UPDATE " + T
                + " SET entity_type_flag=:type,label_id=:owner,entity_id=:entity,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant AND id=:id AND deleted=0";
        return tx.transactional(write(s, v, false)
                .fetch()
                .rowsUpdated()
                .flatMap(n -> n == 1 ? get(v.getTenantId(), v.getId()) : Mono.empty()));
    }

    public Mono<Boolean> delete(Long t, Long id, Long op, String name) {
        return tx.transactional(c.sql(
                        "UPDATE " + T
                                + " SET deleted=1,operator_id=:op,operator_name=:name,operate_time=:time WHERE tenant_id=:tenant AND id=:id AND deleted=0")
                .bind("op", op == null ? 0L : op)
                .bind("name", name == null ? "" : name)
                .bind("time", d.bindInstant(Instant.now()))
                .bind("tenant", t)
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .map(n -> n == 1));
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec s, BindingFilter f) {
        s = s.bind("tenant", f.tenantId());
        if (f.entityType() != null) s = s.bind("type", f.entityType().getIndex());
        if (f.ownerId() != null) s = s.bind("owner", f.ownerId());
        if (f.entityId() != null) s = s.bind("entity", f.entityId());
        return s;
    }

    private DatabaseClient.GenericExecuteSpec write(String s, LabelBindBO v, boolean in) {
        DatabaseClient.GenericExecuteSpec q = c.sql(s)
                .bind("id", v.getId())
                .bind("type", v.getEntityTypeFlag().getIndex())
                .bind("owner", v.getLabelId())
                .bind("entity", v.getEntityId())
                .bind("tenant", v.getTenantId())
                .bind("remark", v.getRemark() == null ? "" : v.getRemark())
                .bind("operator_id", v.getOperatorId() == null ? 0L : v.getOperatorId())
                .bind("operator_name", v.getOperatorName() == null ? "" : v.getOperatorName())
                .bind("operate_time", d.bindInstant(v.getOperateTime().toInstant(ZoneOffset.UTC)));
        if (in)
            q = q.bind("creator_id", v.getCreatorId() == null ? 0L : v.getCreatorId())
                    .bind("creator_name", v.getCreatorName() == null ? "" : v.getCreatorName())
                    .bind("create_time", d.bindInstant(v.getCreateTime().toInstant(ZoneOffset.UTC)));
        return q;
    }

    private LabelBindBO map(io.r2dbc.spi.Row r, io.r2dbc.spi.RowMetadata m) {
        LabelBindBO v = new LabelBindBO();
        v.setId(r.get("id", Long.class));
        v.setEntityTypeFlag(
                EntityTypeEnum.ofIndex(num(r.get("entity_type_flag")).byteValue()));
        v.setLabelId(r.get("label_id", Long.class));
        v.setEntityId(r.get("entity_id", Long.class));
        v.setTenantId(r.get("tenant_id", Long.class));
        v.setRemark(r.get("remark", String.class));
        v.setCreatorId(r.get("creator_id", Long.class));
        v.setCreatorName(r.get("creator_name", String.class));
        v.setCreateTime(time(r.get("create_time")));
        v.setOperatorId(r.get("operator_id", Long.class));
        v.setOperatorName(r.get("operator_name", String.class));
        v.setOperateTime(time(r.get("operate_time")));
        return v;
    }

    private String order(List<SortSpec> s) {
        List<String> o = new ArrayList<>();
        for (SortSpec x : s) {
            String k =
                    switch (x.field()) {
                        case "entityId" -> "entity_id";
                        case "createTime" -> "create_time";
                        case "operateTime" -> "operate_time";
                        case "id" -> "id";
                        default -> throw new IllegalArgumentException("binding sort field is not allowed");
                    };
            o.add(k + (x.direction() == SortSpec.Direction.ASC ? " ASC" : " DESC"));
        }
        if (o.stream().noneMatch(v -> v.startsWith("id "))) o.add("id DESC");
        return String.join(", ", o);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private LocalDateTime time(Object v) {
        if (v instanceof LocalDateTime x) return x;
        if (v instanceof OffsetDateTime x)
            return x.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (v instanceof Instant x) return LocalDateTime.ofInstant(x, ZoneOffset.UTC);
        return null;
    }

    private Number num(Object v) {
        return v instanceof Number n ? n : 0;
    }

    private boolean ok(Long v) {
        return v != null && v > 0;
    }
}
