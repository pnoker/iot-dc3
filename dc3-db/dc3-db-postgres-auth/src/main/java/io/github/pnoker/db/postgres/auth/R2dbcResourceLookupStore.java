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

import io.github.pnoker.common.auth.repository.ReactiveResourceLookupStore;

import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.utils.JsonUtil;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/** Explicit SQL adapter for enabled global resources. */
@Repository
@ConditionalOnClass(DatabaseClient.class)
@RequiredArgsConstructor
public class R2dbcResourceLookupStore implements ReactiveResourceLookupStore {
    private static final String TABLE = "dc3_auth.dc3_resource";
    private static final String COLUMNS =
            "id,parent_resource_id,resource_name,resource_code,service_name,resource_type_flag,resource_scope_flag,entity_id,resource_ext,enable_flag,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted";
    private final DatabaseClient databaseClient;

    @Override
    public Flux<ResourceDO> listEnabledByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Flux.empty();
        Map<String, Long> parameters = new HashMap<>();
        StringBuilder placeholders = new StringBuilder();
        int index = 0;
        for (Long id : ids) {
            if (id == null || id <= 0) continue;
            if (placeholders.length() > 0) placeholders.append(',');
            String name = "resource_id_" + index++;
            placeholders.append(':').append(name);
            parameters.put(name, id);
        }
        if (parameters.isEmpty()) return Flux.empty();
        DatabaseClient.GenericExecuteSpec query = databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                + " WHERE id IN (" + placeholders + ") AND enable_flag=0 AND deleted=0 ORDER BY id");
        for (Map.Entry<String, Long> parameter : parameters.entrySet())
            query = query.bind(parameter.getKey(), parameter.getValue());
        return query.map(this::map).all();
    }

    private ResourceDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        ResourceDO value = new ResourceDO();
        value.setId(row.get("id", Long.class));
        value.setParentResourceId(row.get("parent_resource_id", Long.class));
        value.setResourceName(row.get("resource_name", String.class));
        value.setResourceCode(row.get("resource_code", String.class));
        value.setServiceName(row.get("service_name", String.class));
        value.setResourceTypeFlag(number(row.get("resource_type_flag")));
        value.setResourceScopeFlag(number(row.get("resource_scope_flag")));
        value.setEntityId(row.get("entity_id", Long.class));
        value.setResourceExt(json(row.get("resource_ext", String.class)));
        value.setEnableFlag(number(row.get("enable_flag")));
        value.setRemark(row.get("remark", String.class));
        value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class));
        value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(time(row.get("operate_time")));
        value.setDeleted(number(row.get("deleted")));
        return value;
    }

    private Byte number(Object raw) {
        return raw instanceof Number value ? value.byteValue() : null;
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
}
