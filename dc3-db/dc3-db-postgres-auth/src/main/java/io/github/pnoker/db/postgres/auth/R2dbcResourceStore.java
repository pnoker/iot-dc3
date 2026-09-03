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

import io.github.pnoker.common.auth.repository.ReactiveResourceStore;
import io.github.pnoker.common.auth.repository.ResourceFilter;

import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceScopeTypeEnum;
import io.github.pnoker.common.enums.ResourceTypeEnum;
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
import java.util.Optional;
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
public class R2dbcResourceStore implements ReactiveResourceStore {

    private static final String TABLE = "dc3_auth.dc3_resource";
    private static final String COLUMNS = "id,parent_resource_id,resource_name,resource_code,service_name,"
            + "resource_type_flag,resource_scope_flag,entity_id,resource_ext,enable_flag,remark,creator_id,"
            + "creator_name,create_time,operator_id,operator_name,operate_time,deleted";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<ResourceDO> getById(Long id) {
        if (!valid(id)) {
            return Mono.empty();
        }
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE id=:id AND deleted=0 LIMIT 1")
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<ResourceDO> getByTypeAndEntity(ResourceTypeEnum resourceType, Long entityId) {
        if (resourceType == null || !valid(entityId)) return Mono.empty();
        return databaseClient
                .sql(
                        "SELECT " + COLUMNS + " FROM " + TABLE
                                + " WHERE resource_type_flag=:resource_type_flag AND entity_id=:entity_id AND deleted=0 LIMIT 1")
                .bind("resource_type_flag", resourceType.getIndex())
                .bind("entity_id", entityId)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<OffsetPage<ResourceDO>> list(ResourceFilter filter) {
        if (filter == null) {
            return Mono.error(new IllegalArgumentException("resource filter is required"));
        }
        String where = whereClause(filter);
        DatabaseClient.GenericExecuteSpec count =
                bind(databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + where), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(
                        databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + where + " ORDER BY "
                                + orderBy(filter.page().sort()) + " LIMIT :limit OFFSET :offset"),
                        filter)
                .bind("limit", filter.page().limit())
                .bind("offset", filter.page().offset());
        Mono<Long> total = count.map((row, metadata) -> Optional.ofNullable(row.get("total", Number.class))
                        .map(Number::longValue)
                        .orElse(0L))
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
    public Flux<ResourceDO> listTree(ResourceFilter filter) {
        if (filter == null) {
            return Flux.error(new IllegalArgumentException("resource filter is required"));
        }
        return bind(
                        databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + whereClause(filter) + " ORDER BY "
                                + orderBy(filter.page().sort())),
                        filter)
                .map(this::map)
                .all();
    }

    @Override
    public Mono<ResourceDO> insert(ResourceBO resource) {
        if (resource == null) {
            return Mono.error(new IllegalArgumentException("resource is required"));
        }
        long id = UuidV7.nextLong();
        LocalDateTime now = now();
        String sql = "INSERT INTO " + TABLE + " (id,parent_resource_id,resource_name,resource_code,service_name,"
                + "resource_type_flag,resource_scope_flag,entity_id,resource_ext,enable_flag,remark,creator_id,"
                + "creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES "
                + "(:id,:parent_resource_id,:resource_name,:resource_code,:service_name,:resource_type_flag,"
                + ":resource_scope_flag,:entity_id," + dialect.jsonWriteExpression(":resource_ext")
                + ",:enable_flag,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(sql)
                .bind("id", id)
                .bind("parent_resource_id", value(resource.getParentResourceId()))
                .bind("resource_name", text(resource.getResourceName()))
                .bind("resource_code", text(resource.getResourceCode()))
                .bind("service_name", text(resource.getServiceName()))
                .bind("resource_type_flag", index(resource.getResourceTypeFlag()))
                .bind("resource_scope_flag", index(resource.getResourceScopeFlag()))
                .bind("entity_id", value(resource.getEntityId()))
                .bind("resource_ext", json(resource.getResourceExt()))
                .bind("enable_flag", index(resource.getEnableFlag()))
                .bind("remark", text(resource.getRemark()))
                .bind("creator_id", value(resource.getCreatorId()))
                .bind("creator_name", text(resource.getCreatorName()))
                .bind("create_time", now)
                .bind("operator_id", value(resource.getOperatorId()))
                .bind("operator_name", text(resource.getOperatorName()))
                .bind("operate_time", now);
        return transactionalOperator.transactional(query.fetch().rowsUpdated()).then(getById(id));
    }

    @Override
    public Mono<ResourceDO> update(ResourceBO resource) {
        if (resource == null || !valid(resource.getId())) {
            return Mono.empty();
        }
        String sql = "UPDATE " + TABLE + " SET parent_resource_id=:parent_resource_id,resource_name=:resource_name,"
                + "resource_code=:resource_code,service_name=:service_name,resource_type_flag=:resource_type_flag,"
                + "resource_scope_flag=:resource_scope_flag,entity_id=:entity_id,resource_ext="
                + dialect.jsonWriteExpression(":resource_ext")
                + ",enable_flag=:enable_flag,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,"
                + "operate_time=:operate_time WHERE id=:id AND deleted=0";
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(sql)
                .bind("id", resource.getId())
                .bind("parent_resource_id", value(resource.getParentResourceId()))
                .bind("resource_name", text(resource.getResourceName()))
                .bind("resource_code", text(resource.getResourceCode()))
                .bind("service_name", text(resource.getServiceName()))
                .bind("resource_type_flag", index(resource.getResourceTypeFlag()))
                .bind("resource_scope_flag", index(resource.getResourceScopeFlag()))
                .bind("entity_id", value(resource.getEntityId()))
                .bind("resource_ext", json(resource.getResourceExt()))
                .bind("enable_flag", index(resource.getEnableFlag()))
                .bind("remark", text(resource.getRemark()))
                .bind("operator_id", value(resource.getOperatorId()))
                .bind("operator_name", text(resource.getOperatorName()))
                .bind("operate_time", now());
        return transactionalOperator.transactional(query.fetch().rowsUpdated()).then(getById(resource.getId()));
    }

    @Override
    public Mono<Boolean> delete(Long id, Long operatorId, String operatorName) {
        if (!valid(id)) {
            return Mono.just(false);
        }
        DatabaseClient.GenericExecuteSpec resourceDelete = databaseClient
                .sql("UPDATE " + TABLE
                        + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                        + " WHERE id=:id AND deleted=0")
                .bind("id", id)
                .bind("operator_id", value(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", now());
        DatabaseClient.GenericExecuteSpec bindingDelete = databaseClient
                .sql("UPDATE dc3_auth.dc3_role_resource_bind"
                        + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                        + " WHERE resource_id=:resource_id AND deleted=0")
                .bind("resource_id", id)
                .bind("operator_id", value(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", now());
        return transactionalOperator.transactional(resourceDelete
                .fetch()
                .rowsUpdated()
                .flatMap(updated ->
                        updated == 1 ? bindingDelete.fetch().rowsUpdated().thenReturn(true) : Mono.just(false)));
    }

    @Override
    public Mono<Boolean> existsDuplicate(ResourceBO resource) {
        if (resource == null || isBlank(resource.getResourceCode())) {
            return Mono.just(false);
        }
        String predicate = " WHERE resource_code=:resource_code AND service_name=:service_name AND deleted=0";
        if (resource.getId() != null) predicate += " AND id<>:id";
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql("SELECT 1 FROM " + TABLE + predicate + " LIMIT 1")
                .bind("resource_code", resource.getResourceCode().trim())
                .bind("service_name", text(resource.getServiceName()));
        if (resource.getId() != null) query = query.bind("id", resource.getId());
        return query.fetch().first().hasElement();
    }

    @Override
    public Mono<Boolean> hasChildren(Long id) {
        if (!valid(id)) {
            return Mono.just(false);
        }
        return databaseClient
                .sql("SELECT 1 FROM " + TABLE + " WHERE parent_resource_id=:id AND deleted=0 LIMIT 1")
                .bind("id", id)
                .fetch()
                .first()
                .hasElement();
    }

    @Override
    public Mono<Boolean> isDescendant(Long rootId, Long candidateId) {
        if (!valid(rootId) || !valid(candidateId)) {
            return Mono.just(false);
        }
        String sql = "WITH RECURSIVE descendants(id) AS ("
                + " SELECT id FROM " + TABLE + " WHERE parent_resource_id=:root_id AND deleted=0"
                + " UNION "
                + " SELECT resource.id FROM " + TABLE + " resource"
                + " JOIN descendants parent ON resource.parent_resource_id=parent.id"
                + " WHERE resource.deleted=0)"
                + " SELECT 1 FROM descendants WHERE id=:candidate_id LIMIT 1";
        return databaseClient
                .sql(sql)
                .bind("root_id", rootId)
                .bind("candidate_id", candidateId)
                .fetch()
                .first()
                .hasElement();
    }

    private String whereClause(ResourceFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE deleted=0");
        if (filter.resourceName() != null)
            where.append(" AND ").append(dialect.caseInsensitiveLike("resource_name", ":resource_name"));
        if (filter.resourceCode() != null) where.append(" AND resource_code=:resource_code");
        if (filter.parentResourceId() != null) where.append(" AND parent_resource_id=:parent_resource_id");
        if (filter.enableFlag() != null) where.append(" AND enable_flag=:enable_flag");
        if (!filter.resourceTypeFlags().isEmpty())
            where.append(" AND resource_type_flag IN (")
                    .append(placeholders(
                            "resource_type_flag", filter.resourceTypeFlags().size()))
                    .append(')');
        if (!filter.resourceScopeFlags().isEmpty())
            where.append(" AND resource_scope_flag IN (")
                    .append(placeholders(
                            "resource_scope_flag", filter.resourceScopeFlags().size()))
                    .append(')');
        return where.toString();
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, ResourceFilter filter) {
        if (filter.resourceName() != null) spec = spec.bind("resource_name", "%" + filter.resourceName() + "%");
        if (filter.resourceCode() != null) spec = spec.bind("resource_code", filter.resourceCode());
        if (filter.parentResourceId() != null) spec = spec.bind("parent_resource_id", filter.parentResourceId());
        if (filter.enableFlag() != null)
            spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        for (int i = 0; i < filter.resourceTypeFlags().size(); i++) {
            spec = spec.bind(
                    "resource_type_flag" + i, filter.resourceTypeFlags().get(i).getIndex());
        }
        for (int i = 0; i < filter.resourceScopeFlags().size(); i++) {
            spec = spec.bind(
                    "resource_scope_flag" + i,
                    filter.resourceScopeFlags().get(i).getIndex());
        }
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        List<String> clauses = new ArrayList<>();
        if (sort != null) {
            for (SortSpec spec : sort) {
                String column =
                        switch (spec.field()) {
                            case "id" -> "id";
                            case "resourceName" -> "resource_name";
                            case "resourceCode" -> "resource_code";
                            case "serviceName" -> "service_name";
                            case "resourceTypeFlag" -> "resource_type_flag";
                            case "resourceScopeFlag" -> "resource_scope_flag";
                            case "enableFlag" -> "enable_flag";
                            default ->
                                throw new IllegalArgumentException("unsupported resource sort field: " + spec.field());
                        };
                clauses.add(column + " " + spec.direction().name());
            }
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("id "))) clauses.add("id ASC");
        return String.join(",", clauses);
    }

    private ResourceDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        ResourceDO resource = new ResourceDO();
        resource.setId(row.get("id", Long.class));
        resource.setParentResourceId(row.get("parent_resource_id", Long.class));
        resource.setResourceName(row.get("resource_name", String.class));
        resource.setResourceCode(row.get("resource_code", String.class));
        resource.setServiceName(row.get("service_name", String.class));
        resource.setResourceTypeFlag(number(row.get("resource_type_flag")));
        resource.setResourceScopeFlag(number(row.get("resource_scope_flag")));
        resource.setEntityId(row.get("entity_id", Long.class));
        resource.setResourceExt(parseJson(row.get("resource_ext", String.class)));
        resource.setEnableFlag(number(row.get("enable_flag")));
        resource.setRemark(row.get("remark", String.class));
        resource.setCreatorId(row.get("creator_id", Long.class));
        resource.setCreatorName(row.get("creator_name", String.class));
        resource.setCreateTime(time(row.get("create_time")));
        resource.setOperatorId(row.get("operator_id", Long.class));
        resource.setOperatorName(row.get("operator_name", String.class));
        resource.setOperateTime(time(row.get("operate_time")));
        resource.setDeleted(number(row.get("deleted")));
        return resource;
    }

    private JsonExt parseJson(String value) {
        if (value == null) return null;
        try {
            return JsonUtil.parseObject(value, JsonExt.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Byte number(Object value) {
        return value instanceof Number number ? number.byteValue() : null;
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return null;
    }

    private String json(Object value) {
        return value == null ? "{}" : JsonUtil.toJsonString(value);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private byte index(Object value) {
        if (value instanceof EnableFlagEnum flag) return flag.getIndex();
        if (value instanceof ResourceTypeEnum flag) return flag.getIndex();
        if (value instanceof ResourceScopeTypeEnum flag) return flag.getIndex();
        return 0;
    }

    private String placeholders(String prefix, int size) {
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(index -> ":" + prefix + index)
                .reduce((left, right) -> left + "," + right)
                .orElseThrow();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private boolean valid(Long value) {
        return value != null && value > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
