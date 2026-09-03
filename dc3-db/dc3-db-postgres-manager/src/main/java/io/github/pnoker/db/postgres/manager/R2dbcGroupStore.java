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
package io.github.pnoker.db.postgres.manager;

import io.github.pnoker.common.manager.repository.GroupFilter;
import io.github.pnoker.common.manager.repository.ReactiveGroupStore;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.manager.entity.bo.GroupBO;
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

/** R2DBC group repository shared by PostgreSQL, MySQL and MariaDB. */
@Repository
@RequiredArgsConstructor
@ConditionalOnClass({DatabaseClient.class, R2dbcDialect.class, TransactionalOperator.class})
public class R2dbcGroupStore implements ReactiveGroupStore {
    private static final String TABLE = "dc3_manager.dc3_group";
    private static final String COLUMNS =
            "id,parent_group_id,group_name,group_code,group_level,group_index,entity_type_flag,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time";
    private final DatabaseClient client;
    private final R2dbcDialect dialect;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;

    @Override
    public Mono<OffsetPage<GroupBO>> list(GroupFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE tenant_id=:tenant_id AND deleted=0");
        if (text(filter.groupName()))
            where.append(" AND ").append(dialect.caseInsensitiveLike("group_name", ":group_name"));
        if (filter.parentGroupId() != null) where.append(" AND parent_group_id=:parent_id");
        if (filter.position() != null) where.append(" AND group_index=:position");
        if (filter.groupTypeFlag() != null) where.append(" AND entity_type_flag=:entity_type");
        if (filter.enableFlag() != null) where.append(" AND enable_flag=:enable_flag");
        Mono<List<GroupBO>> items = bind(
                        client.sql("SELECT " + COLUMNS + " FROM " + TABLE + where + " ORDER BY "
                                + orderBy(filter.sort()) + " LIMIT :limit OFFSET :offset"),
                        filter)
                .bind("limit", filter.limit())
                .bind("offset", filter.offset())
                .map(this::map)
                .all()
                .collectList();
        Mono<Long> total = bind(client.sql("SELECT COUNT(*) AS total FROM " + TABLE + where), filter)
                .map((row, meta) -> number(row.get("total")).longValue())
                .one()
                .defaultIfEmpty(0L);
        return total.flatMap(totalCount -> items.map(pageItems -> new OffsetPage<>(
                        pageItems,
                        filter.offset(),
                        filter.limit(),
                        totalCount,
                        filter.offset() + pageItems.size() < totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<GroupBO> get(Long tenantId, Long id) {
        if (!valid(tenantId) || !valid(id)) return Mono.empty();
        return client.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<GroupBO> getByName(Long tenantId, byte type, Long parentId, String name) {
        if (!valid(tenantId) || !text(name)) return Mono.empty();
        return client.sql(
                        "SELECT " + COLUMNS + " FROM " + TABLE
                                + " WHERE tenant_id=:tenant_id AND entity_type_flag=:type AND parent_group_id=:parent_id AND group_name=:name AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("type", type)
                .bind("parent_id", parentId == null ? 0L : parentId)
                .bind("name", name)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<Boolean> hasChildren(Long tenantId, Long id) {
        return exists(
                "SELECT 1 FROM " + TABLE + " WHERE tenant_id=:tenant_id AND parent_group_id=:id AND deleted=0 LIMIT 1",
                tenantId,
                id);
    }

    @Override
    public Mono<Boolean> hasActiveBindings(Long tenantId, Long id) {
        return exists(
                "SELECT 1 FROM dc3_manager.dc3_group_bind WHERE tenant_id=:tenant_id AND group_id=:id AND deleted=0 LIMIT 1",
                tenantId,
                id);
    }

    @Override
    public Mono<GroupBO> insert(GroupBO value) {
        value.setId(value.getId() == null ? UuidV7.nextLong() : value.getId());
        value.setCreateTime(now());
        value.setOperateTime(value.getCreateTime());
        String sql = "INSERT INTO " + TABLE
                + " (id,parent_group_id,group_name,group_code,group_level,group_index,entity_type_flag,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:parent_id,:name,:code,:level,:index,:type,:enable,:tenant_id,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        return transactionalOperator.transactional(write(sql, value, true)
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }

    @Override
    public Mono<GroupBO> update(GroupBO value) {
        value.setOperateTime(now());
        String sql = "UPDATE " + TABLE
                + " SET parent_group_id=:parent_id,group_name=:name,group_code=:code,group_level=:level,group_index=:index,entity_type_flag=:type,enable_flag=:enable,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0";
        return transactionalOperator.transactional(write(sql, value, false)
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (!valid(tenantId) || !valid(id)) return Mono.just(false);
        return transactionalOperator.transactional(client.sql(
                        "UPDATE " + TABLE
                                + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0")
                .bind("operator_id", operatorId == null ? 0L : operatorId)
                .bind("operator_name", operatorName == null ? "" : operatorName)
                .bind("operate_time", dialect.bindInstant(Instant.now()))
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1));
    }

    private Mono<Boolean> exists(String sql, Long tenantId, Long id) {
        if (!valid(tenantId) || !valid(id)) return Mono.just(false);
        return client.sql(sql)
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map((row, meta) -> true)
                .one()
                .defaultIfEmpty(false);
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, GroupFilter f) {
        spec = spec.bind("tenant_id", f.tenantId());
        if (text(f.groupName()))
            spec = spec.bind("group_name", "%" + f.groupName().trim() + "%");
        if (f.parentGroupId() != null) spec = spec.bind("parent_id", f.parentGroupId());
        if (f.position() != null) spec = spec.bind("position", f.position());
        if (f.groupTypeFlag() != null)
            spec = spec.bind("entity_type", f.groupTypeFlag().getIndex());
        if (f.enableFlag() != null)
            spec = spec.bind("enable_flag", f.enableFlag().getIndex());
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec write(String sql, GroupBO v, boolean insert) {
        DatabaseClient.GenericExecuteSpec spec = client.sql(sql)
                .bind("id", v.getId())
                .bind("parent_id", v.getParentGroupId() == null ? 0L : v.getParentGroupId())
                .bind("name", v.getGroupName())
                .bind("code", v.getGroupCode())
                .bind("level", v.getGroupLevel() == null ? 0 : v.getGroupLevel())
                .bind("index", v.getGroupIndex() == null ? 0 : v.getGroupIndex())
                .bind("type", v.getGroupTypeFlag().getIndex())
                .bind(
                        "enable",
                        v.getEnableFlag() == null ? 0 : v.getEnableFlag().getIndex())
                .bind("tenant_id", v.getTenantId())
                .bind("remark", v.getRemark() == null ? "" : v.getRemark())
                .bind("operator_id", v.getOperatorId() == null ? 0L : v.getOperatorId())
                .bind("operator_name", v.getOperatorName() == null ? "" : v.getOperatorName())
                .bind("operate_time", dialect.bindInstant(toInstant(v.getOperateTime())));
        if (insert)
            spec = spec.bind("creator_id", v.getCreatorId() == null ? 0L : v.getCreatorId())
                    .bind("creator_name", v.getCreatorName() == null ? "" : v.getCreatorName())
                    .bind("create_time", dialect.bindInstant(toInstant(v.getCreateTime())));
        return spec;
    }

    private GroupBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata meta) {
        GroupBO v = new GroupBO();
        v.setId(row.get("id", Long.class));
        v.setParentGroupId(row.get("parent_group_id", Long.class));
        v.setGroupName(row.get("group_name", String.class));
        v.setGroupCode(row.get("group_code", String.class));
        v.setGroupLevel(number(row.get("group_level")).byteValue());
        v.setGroupIndex(number(row.get("group_index")).byteValue());
        v.setGroupTypeFlag(
                EntityTypeEnum.ofIndex(number(row.get("entity_type_flag")).byteValue()));
        v.setEnableFlag(EnableFlagEnum.ofIndex(number(row.get("enable_flag")).byteValue()));
        v.setTenantId(row.get("tenant_id", Long.class));
        v.setRemark(row.get("remark", String.class));
        v.setCreatorId(row.get("creator_id", Long.class));
        v.setCreatorName(row.get("creator_name", String.class));
        v.setCreateTime(time(row.get("create_time")));
        v.setOperatorId(row.get("operator_id", Long.class));
        v.setOperatorName(row.get("operator_name", String.class));
        v.setOperateTime(time(row.get("operate_time")));
        return v;
    }

    private String orderBy(List<SortSpec> sort) {
        List<String> out = new ArrayList<>();
        for (SortSpec s : sort) {
            String c =
                    switch (s.field()) {
                        case "groupName" -> "group_name";
                        case "groupCode" -> "group_code";
                        case "groupLevel" -> "group_level";
                        case "groupIndex" -> "group_index";
                        case "createTime" -> "create_time";
                        case "operateTime" -> "operate_time";
                        case "id" -> "id";
                        default -> throw new IllegalArgumentException("group sort field is not allowed");
                    };
            out.add(c + (s.direction() == SortSpec.Direction.ASC ? " ASC" : " DESC"));
        }
        if (out.stream().noneMatch(v -> v.startsWith("id "))) out.add("id DESC");
        return String.join(", ", out);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime v) {
        return v.toInstant(ZoneOffset.UTC);
    }

    private LocalDateTime time(Object v) {
        if (v instanceof LocalDateTime x) return x;
        if (v instanceof OffsetDateTime x)
            return x.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (v instanceof Instant x) return LocalDateTime.ofInstant(x, ZoneOffset.UTC);
        return null;
    }

    private Number number(Object v) {
        return v instanceof Number n ? n : 0;
    }

    private boolean text(String v) {
        return v != null && !v.isBlank();
    }

    private boolean valid(Long v) {
        return v != null && v > 0;
    }
}
