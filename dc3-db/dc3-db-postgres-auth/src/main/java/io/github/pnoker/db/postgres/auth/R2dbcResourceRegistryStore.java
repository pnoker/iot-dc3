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

import io.github.pnoker.common.auth.repository.ReactiveResourceRegistryStore;

import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for resource registry records. */
@Repository
@ConditionalOnClass({DatabaseClient.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcResourceRegistryStore implements ReactiveResourceRegistryStore {

    private static final String API_TABLE = "dc3_auth.dc3_api";
    private static final String RESOURCE_TABLE = "dc3_auth.dc3_resource";
    private static final String API_COLUMNS = "id,service_name,api_type_flag,api_name,api_code,api_group,api_ext,"
            + "enable_flag,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted";
    private static final String RESOURCE_COLUMNS = "id,parent_resource_id,resource_name,resource_code,service_name,"
            + "resource_type_flag,resource_scope_flag,entity_id,resource_ext,enable_flag,remark,creator_id,creator_name,"
            + "create_time,operator_id,operator_name,operate_time,deleted";

    private final DatabaseClient databaseClient;
    private final R2dbcDialect dialect;

    @Override
    public Flux<ApiDO> listApis(String serviceName) {
        return databaseClient
                .sql("SELECT " + API_COLUMNS + " FROM " + API_TABLE
                        + " WHERE service_name=:service_name AND deleted=0 ORDER BY id ASC")
                .bind("service_name", serviceName)
                .map(this::mapApi)
                .all();
    }

    @Override
    public Mono<ApiDO> insertApi(ApiDO api) {
        long id = newId();
        LocalDateTime now = now();
        String sql = "INSERT INTO " + API_TABLE + " (id,service_name,api_type_flag,api_name,api_code,api_group,api_ext,"
                + "enable_flag,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                + " VALUES (:id,:service_name,:api_type_flag,:api_name,:api_code,:api_group,"
                + dialect.jsonWriteExpression(":api_ext")
                + ",:enable_flag,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(sql)
                .bind("id", id)
                .bind("service_name", text(api.getServiceName()))
                .bind("api_type_flag", value(api.getApiTypeFlag()))
                .bind("api_name", text(api.getApiName()))
                .bind("api_code", text(api.getApiCode()))
                .bind("api_group", text(api.getApiGroup()))
                .bind("api_ext", json(api.getApiExt()))
                .bind("enable_flag", value(api.getEnableFlag()))
                .bind("remark", text(api.getRemark()))
                .bind("creator_id", longValue(api.getCreatorId()))
                .bind("creator_name", text(api.getCreatorName()))
                .bind("create_time", now)
                .bind("operator_id", longValue(api.getOperatorId()))
                .bind("operator_name", text(api.getOperatorName()))
                .bind("operate_time", now);
        return query.fetch()
                .rowsUpdated()
                .flatMap(updated -> updated == 1
                        ? getApiById(id)
                        : Mono.error(new IllegalStateException("API insert affected no rows")));
    }

    @Override
    public Mono<ApiDO> updateApi(ApiDO api) {
        if (api == null || api.getId() == null) return Mono.empty();
        String sql = "UPDATE " + API_TABLE + " SET service_name=:service_name,api_type_flag=:api_type_flag,"
                + "api_name=:api_name,api_code=:api_code,api_group=:api_group,api_ext="
                + dialect.jsonWriteExpression(":api_ext")
                + ",enable_flag=:enable_flag,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,"
                + "operate_time=:operate_time WHERE id=:id AND deleted=0";
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(sql)
                .bind("id", api.getId())
                .bind("service_name", text(api.getServiceName()))
                .bind("api_type_flag", value(api.getApiTypeFlag()))
                .bind("api_name", text(api.getApiName()))
                .bind("api_code", text(api.getApiCode()))
                .bind("api_group", text(api.getApiGroup()))
                .bind("api_ext", json(api.getApiExt()))
                .bind("enable_flag", value(api.getEnableFlag()))
                .bind("remark", text(api.getRemark()))
                .bind("operator_id", longValue(api.getOperatorId()))
                .bind("operator_name", text(api.getOperatorName()))
                .bind("operate_time", now());
        return query.fetch().rowsUpdated().flatMap(updated -> updated == 1 ? getApiById(api.getId()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> deleteApi(Long id, Long operatorId, String operatorName) {
        if (id == null) return Mono.just(false);
        return databaseClient
                .sql("UPDATE " + API_TABLE
                        + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                        + " WHERE id=:id AND deleted=0")
                .bind("id", id)
                .bind("operator_id", longValue(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", now())
                .fetch()
                .rowsUpdated()
                .map(updated -> updated == 1);
    }

    @Override
    public Flux<ResourceDO> listApiResources(String serviceName) {
        return databaseClient
                .sql("SELECT " + RESOURCE_COLUMNS + " FROM " + RESOURCE_TABLE
                        + " WHERE (service_name=:service_name OR resource_code=:service_code OR resource_code LIKE :group_prefix)"
                        + " AND resource_type_flag=:resource_type_flag AND deleted=0"
                        + " ORDER BY id ASC")
                .bind("service_name", serviceName)
                .bind("service_code", "api:service:" + serviceName)
                .bind("group_prefix", "api:group:" + serviceName + ":%")
                .bind("resource_type_flag", (byte) 6)
                .map(this::mapResource)
                .all();
    }

    @Override
    public Flux<ResourceDO> listResourcesByEntityIds(List<Long> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) return Flux.empty();
        return Flux.defer(() -> {
            String sql = "SELECT " + RESOURCE_COLUMNS + " FROM " + RESOURCE_TABLE
                    + " WHERE deleted=0 AND entity_id IN (" + placeholders(entityIds.size()) + ")";
            DatabaseClient.GenericExecuteSpec bound = databaseClient.sql(sql);
            for (int i = 0; i < entityIds.size(); i++) bound = bound.bind("entity_id" + i, entityIds.get(i));
            return bound.map(this::mapResource).all();
        });
    }

    @Override
    public Mono<ResourceDO> getResourceByCode(String resourceCode) {
        if (resourceCode == null || resourceCode.isBlank()) return Mono.empty();
        return databaseClient
                .sql("SELECT " + RESOURCE_COLUMNS + " FROM " + RESOURCE_TABLE
                        + " WHERE resource_code=:resource_code AND deleted=0 ORDER BY id ASC LIMIT 1")
                .bind("resource_code", resourceCode)
                .map(this::mapResource)
                .one();
    }

    @Override
    public Mono<ResourceDO> insertResource(ResourceDO resource) {
        long id = newId();
        LocalDateTime now = now();
        String sql =
                "INSERT INTO " + RESOURCE_TABLE + " (id,parent_resource_id,resource_name,resource_code,service_name,"
                        + "resource_type_flag,resource_scope_flag,entity_id,resource_ext,enable_flag,remark,creator_id,creator_name,"
                        + "create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:parent_resource_id,:resource_name,"
                        + ":resource_code,:service_name,:resource_type_flag,:resource_scope_flag,:entity_id,"
                        + dialect.jsonWriteExpression(":resource_ext")
                        + ",:enable_flag,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(sql)
                .bind("id", id)
                .bind("parent_resource_id", longValue(resource.getParentResourceId()))
                .bind("resource_name", text(resource.getResourceName()))
                .bind("resource_code", text(resource.getResourceCode()))
                .bind("service_name", text(resource.getServiceName()))
                .bind("resource_type_flag", value(resource.getResourceTypeFlag()))
                .bind("resource_scope_flag", value(resource.getResourceScopeFlag()))
                .bind("entity_id", longValue(resource.getEntityId()))
                .bind("resource_ext", json(resource.getResourceExt()))
                .bind("enable_flag", value(resource.getEnableFlag()))
                .bind("remark", text(resource.getRemark()))
                .bind("creator_id", longValue(resource.getCreatorId()))
                .bind("creator_name", text(resource.getCreatorName()))
                .bind("create_time", now)
                .bind("operator_id", longValue(resource.getOperatorId()))
                .bind("operator_name", text(resource.getOperatorName()))
                .bind("operate_time", now);
        return query.fetch()
                .rowsUpdated()
                .flatMap(updated -> updated == 1
                        ? getResourceById(id)
                        : Mono.error(new IllegalStateException("resource insert affected no rows")));
    }

    @Override
    public Mono<ResourceDO> updateResource(ResourceDO resource) {
        if (resource == null || resource.getId() == null) return Mono.empty();
        String sql =
                "UPDATE " + RESOURCE_TABLE + " SET parent_resource_id=:parent_resource_id,resource_name=:resource_name,"
                        + "resource_code=:resource_code,service_name=:service_name,resource_type_flag=:resource_type_flag,"
                        + "resource_scope_flag=:resource_scope_flag,entity_id=:entity_id,resource_ext="
                        + dialect.jsonWriteExpression(":resource_ext")
                        + ",enable_flag=:enable_flag,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,"
                        + "operate_time=:operate_time WHERE id=:id AND deleted=0";
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(sql)
                .bind("id", resource.getId())
                .bind("parent_resource_id", longValue(resource.getParentResourceId()))
                .bind("resource_name", text(resource.getResourceName()))
                .bind("resource_code", text(resource.getResourceCode()))
                .bind("service_name", text(resource.getServiceName()))
                .bind("resource_type_flag", value(resource.getResourceTypeFlag()))
                .bind("resource_scope_flag", value(resource.getResourceScopeFlag()))
                .bind("entity_id", longValue(resource.getEntityId()))
                .bind("resource_ext", json(resource.getResourceExt()))
                .bind("enable_flag", value(resource.getEnableFlag()))
                .bind("remark", text(resource.getRemark()))
                .bind("operator_id", longValue(resource.getOperatorId()))
                .bind("operator_name", text(resource.getOperatorName()))
                .bind("operate_time", now());
        return query.fetch()
                .rowsUpdated()
                .flatMap(updated -> updated == 1 ? getResourceById(resource.getId()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> deleteResource(Long id, Long operatorId, String operatorName) {
        if (id == null) return Mono.just(false);
        LocalDateTime now = now();
        DatabaseClient.GenericExecuteSpec resource = databaseClient
                .sql("UPDATE " + RESOURCE_TABLE
                        + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                        + " WHERE id=:id AND deleted=0")
                .bind("id", id)
                .bind("operator_id", longValue(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", now);
        DatabaseClient.GenericExecuteSpec bindings = databaseClient
                .sql("UPDATE dc3_auth.dc3_role_resource_bind"
                        + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                        + " WHERE resource_id=:resource_id AND deleted=0")
                .bind("resource_id", id)
                .bind("operator_id", longValue(operatorId))
                .bind("operator_name", text(operatorName))
                .bind("operate_time", now);
        return resource.fetch()
                .rowsUpdated()
                .flatMap(
                        updated -> updated == 1 ? bindings.fetch().rowsUpdated().thenReturn(true) : Mono.just(false));
    }

    @Override
    public Mono<Long> countChildren(Long parentId) {
        return databaseClient
                .sql("SELECT COUNT(*) AS total FROM " + RESOURCE_TABLE
                        + " WHERE parent_resource_id=:parent_resource_id AND deleted=0")
                .bind("parent_resource_id", parentId)
                .map((row, metadata) -> ((Number) row.get("total")).longValue())
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Long> acquireLock(String lockName) {
        String insert = dialect.name().toLowerCase().contains("postgres")
                ? "INSERT INTO dc3_platform_lock(lock_name,fencing_token,expires_at) VALUES (:lock_name,0,:expires_at) ON CONFLICT (lock_name) DO NOTHING"
                : "INSERT IGNORE INTO dc3_platform_lock(lock_name,fencing_token,expires_at) VALUES (:lock_name,0,:expires_at)";
        Instant expires = Instant.now().plusSeconds(300);
        return databaseClient
                .sql(insert)
                .bind("lock_name", lockName)
                .bind("expires_at", dialect.bindInstant(expires))
                .fetch()
                .rowsUpdated()
                .then(databaseClient
                        .sql("SELECT fencing_token FROM dc3_platform_lock WHERE lock_name=:lock_name FOR UPDATE")
                        .bind("lock_name", lockName)
                        .map((row, metadata) -> ((Number) row.get("fencing_token")).longValue())
                        .one())
                .flatMap(current -> databaseClient
                        .sql("UPDATE dc3_platform_lock SET fencing_token=:next_token,expires_at=:expires_at"
                                + " WHERE lock_name=:lock_name")
                        .bind("next_token", current + 1)
                        .bind("expires_at", dialect.bindInstant(expires))
                        .bind("lock_name", lockName)
                        .fetch()
                        .rowsUpdated()
                        .thenReturn(current + 1));
    }

    private Mono<ApiDO> getApiById(Long id) {
        return databaseClient
                .sql("SELECT " + API_COLUMNS + " FROM " + API_TABLE + " WHERE id=:id AND deleted=0")
                .bind("id", id)
                .map(this::mapApi)
                .one();
    }

    private Mono<ResourceDO> getResourceById(Long id) {
        return databaseClient
                .sql("SELECT " + RESOURCE_COLUMNS + " FROM " + RESOURCE_TABLE + " WHERE id=:id AND deleted=0")
                .bind("id", id)
                .map(this::mapResource)
                .one();
    }

    private String placeholders(int size) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) value.append(',');
            value.append(":entity_id").append(i);
        }
        return value.toString();
    }

    private ApiDO mapApi(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        ApiDO api = new ApiDO();
        api.setId(row.get("id", Long.class));
        api.setServiceName(row.get("service_name", String.class));
        api.setApiTypeFlag(number(row.get("api_type_flag")));
        api.setApiName(row.get("api_name", String.class));
        api.setApiCode(row.get("api_code", String.class));
        api.setApiGroup(row.get("api_group", String.class));
        api.setApiExt(parseJson(row.get("api_ext", String.class)));
        api.setEnableFlag(number(row.get("enable_flag")));
        api.setRemark(row.get("remark", String.class));
        api.setCreatorId(row.get("creator_id", Long.class));
        api.setCreatorName(row.get("creator_name", String.class));
        api.setCreateTime(time(row.get("create_time")));
        api.setOperatorId(row.get("operator_id", Long.class));
        api.setOperatorName(row.get("operator_name", String.class));
        api.setOperateTime(time(row.get("operate_time")));
        api.setDeleted(number(row.get("deleted")));
        return api;
    }

    private ResourceDO mapResource(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
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

    private String json(Object value) {
        return value == null ? "{}" : JsonUtil.toJsonString(value);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private long longValue(Long value) {
        return value == null ? 0L : value;
    }

    private byte value(Byte value) {
        return value == null ? 0 : value;
    }

    private long newId() {
        return UuidV7.nextLong();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
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
}
