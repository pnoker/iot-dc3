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
package io.github.pnoker.common.agentic.repository;

import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.utils.UuidV7;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for agentic model configurations. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcModelConfigStore implements ReactiveModelConfigStore {

    private static final String TABLE = "dc3_agentic.dc3_model_config";
    private static final String COLUMNS = "c.id, c.model, c.label, c.provider_id, c.stream, c.tool_call, c.vision,"
            + " c.reasoning, c.temperature, c.max_tokens, c.default_flag, c.enable_flag, c.tenant_id, c.remark,"
            + " c.creator_id, c.creator_name, c.create_time, c.operator_id, c.operator_name, c.operate_time";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Flux<ModelConfigBO> list(RequestHeader.PrincipalHeader header, boolean enabledOnly) {
        validateHeader(header);
        String enabled = enabledOnly ? " AND c.enable_flag = 0" : "";
        return databaseClient
                .sql("SELECT " + COLUMNS + ", p.name AS provider_name FROM " + TABLE
                        + " c LEFT JOIN dc3_agentic.dc3_model_provider p ON p.id = c.provider_id"
                        + " AND p.tenant_id = c.tenant_id AND p.deleted = 0"
                        + " WHERE c.tenant_id = :tenant_id AND c.deleted = 0" + enabled
                        + " ORDER BY c.default_flag DESC, c.model ASC, c.id DESC")
                .bind("tenant_id", header.getTenantId())
                .map(this::map)
                .all();
    }

    @Override
    public Mono<ModelConfigBO> get(Long id, RequestHeader.PrincipalHeader header) {
        validateHeader(header);
        if (id == null) return Mono.error(new IllegalArgumentException("model config id must not be null"));
        return databaseClient
                .sql("SELECT " + COLUMNS + ", p.name AS provider_name FROM " + TABLE
                        + " c LEFT JOIN dc3_agentic.dc3_model_provider p ON p.id = c.provider_id"
                        + " AND p.tenant_id = c.tenant_id AND p.deleted = 0"
                        + " WHERE c.id = :id AND c.tenant_id = :tenant_id AND c.deleted = 0 LIMIT 1")
                .bind("id", id)
                .bind("tenant_id", header.getTenantId())
                .map(this::map)
                .one();
    }

    @Override
    public Mono<ModelConfigBO> findByModel(String model, RequestHeader.PrincipalHeader header) {
        validateHeader(header);
        if (model == null || model.isBlank()) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + ", p.name AS provider_name FROM " + TABLE
                        + " c LEFT JOIN dc3_agentic.dc3_model_provider p ON p.id = c.provider_id"
                        + " AND p.tenant_id = c.tenant_id AND p.deleted = 0"
                        + " WHERE c.model = :model AND c.tenant_id = :tenant_id AND c.enable_flag = 0"
                        + " AND c.deleted = 0 ORDER BY c.default_flag DESC, c.id DESC LIMIT 1")
                .bind("model", model)
                .bind("tenant_id", header.getTenantId())
                .map(this::map)
                .one();
    }

    @Override
    public Mono<ModelConfigBO> findDefault(RequestHeader.PrincipalHeader header) {
        validateHeader(header);
        return databaseClient
                .sql("SELECT " + COLUMNS + ", p.name AS provider_name FROM " + TABLE
                        + " c LEFT JOIN dc3_agentic.dc3_model_provider p ON p.id = c.provider_id"
                        + " AND p.tenant_id = c.tenant_id AND p.deleted = 0"
                        + " WHERE c.tenant_id = :tenant_id AND c.default_flag = 1 AND c.enable_flag = 0"
                        + " AND c.deleted = 0 ORDER BY c.id DESC LIMIT 1")
                .bind("tenant_id", header.getTenantId())
                .map(this::map)
                .one();
    }

    @Override
    public Mono<ModelConfigBO> insert(ModelConfigBO config, RequestHeader.PrincipalHeader header) {
        validateHeader(header);
        ModelConfigBO value = copyWithIdentity(config, header, UuidV7.nextLong());
        Mono<Void> normalize = value.getDefaultFlag() == DefaultFlagEnum.DEFAULT
                ? clearDefaults(value.getTenantId(), value.getId())
                : Mono.empty();
        Mono<ModelConfigBO> write = normalize.then(databaseClient
                .sql("INSERT INTO " + TABLE
                        + " (id, model, label, provider_id, stream, tool_call, vision, reasoning, temperature, max_tokens,"
                        + " default_flag, enable_flag, tenant_id, remark, creator_id, creator_name, create_time,"
                        + " operator_id, operator_name, operate_time, deleted) VALUES (:id, :model, :label, :provider_id,"
                        + " :stream, :tool_call, :vision, :reasoning, :temperature, :max_tokens, :default_flag,"
                        + " :enable_flag, :tenant_id, :remark, :creator_id, :creator_name, :create_time, :operator_id,"
                        + " :operator_name, :operate_time, 0)")
                .bind("id", value.getId())
                .bind("model", value.getModel())
                .bind("label", value.getLabel())
                .bind("provider_id", value.getProviderId())
                .bind("stream", Boolean.TRUE.equals(value.getStream()))
                .bind("tool_call", Boolean.TRUE.equals(value.getToolCall()))
                .bind("vision", Boolean.TRUE.equals(value.getVision()))
                .bind("reasoning", Boolean.TRUE.equals(value.getReasoning()))
                .bind("temperature", value.getTemperature())
                .bind("max_tokens", value.getMaxTokens())
                .bind("default_flag", defaultFlag(value))
                .bind("enable_flag", enableFlag(value))
                .bind("tenant_id", value.getTenantId())
                .bind("remark", value.getRemark())
                .bind("creator_id", value.getCreatorId())
                .bind("creator_name", value.getCreatorName())
                .bind("create_time", utc(value.getCreateTime()))
                .bind("operator_id", value.getOperatorId())
                .bind("operator_name", value.getOperatorName())
                .bind("operate_time", utc(value.getOperateTime()))
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? get(value.getId(), header)
                        : Mono.error(new IllegalStateException("model config insert affected " + rows + " rows"))));
        return transactionalOperator.transactional(write);
    }

    @Override
    public Mono<ModelConfigBO> update(ModelConfigBO config, RequestHeader.PrincipalHeader header) {
        validateHeader(header);
        if (config == null || config.getId() == null) {
            return Mono.error(new IllegalArgumentException("model config id must not be null"));
        }
        ModelConfigBO value = copyWithIdentity(config, header, config.getId());
        Mono<Void> normalize = value.getDefaultFlag() == DefaultFlagEnum.DEFAULT
                ? clearDefaults(value.getTenantId(), value.getId())
                : Mono.empty();
        Mono<ModelConfigBO> write = normalize.then(databaseClient
                .sql("UPDATE " + TABLE + " SET"
                        + " model = :model, label = :label, provider_id = :provider_id, stream = :stream,"
                        + " tool_call = :tool_call, vision = :vision, reasoning = :reasoning, temperature = :temperature,"
                        + " max_tokens = :max_tokens, default_flag = :default_flag, enable_flag = :enable_flag,"
                        + " remark = :remark, operator_id = :operator_id, operator_name = :operator_name,"
                        + " operate_time = :operate_time WHERE id = :id AND tenant_id = :tenant_id AND deleted = 0")
                .bind("model", value.getModel())
                .bind("label", value.getLabel())
                .bind("provider_id", value.getProviderId())
                .bind("stream", Boolean.TRUE.equals(value.getStream()))
                .bind("tool_call", Boolean.TRUE.equals(value.getToolCall()))
                .bind("vision", Boolean.TRUE.equals(value.getVision()))
                .bind("reasoning", Boolean.TRUE.equals(value.getReasoning()))
                .bind("temperature", value.getTemperature())
                .bind("max_tokens", value.getMaxTokens())
                .bind("default_flag", defaultFlag(value))
                .bind("enable_flag", enableFlag(value))
                .bind("remark", value.getRemark())
                .bind("operator_id", value.getOperatorId())
                .bind("operator_name", value.getOperatorName())
                .bind("operate_time", utc(value.getOperateTime()))
                .bind("id", value.getId())
                .bind("tenant_id", value.getTenantId())
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getId(), header) : Mono.empty()));
        return transactionalOperator.transactional(write);
    }

    @Override
    public Mono<Boolean> delete(Long id, RequestHeader.PrincipalHeader header) {
        validateHeader(header);
        if (id == null) return Mono.error(new IllegalArgumentException("model config id must not be null"));
        return databaseClient
                .sql("UPDATE " + TABLE + " SET deleted = 1, operator_id = :operator_id,"
                        + " operate_time = :operate_time WHERE id = :id AND tenant_id = :tenant_id AND deleted = 0")
                .bind("operator_id", header.getUserId())
                .bind("operate_time", utcNow())
                .bind("id", id)
                .bind("tenant_id", header.getTenantId())
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1);
    }

    private Mono<Void> clearDefaults(Long tenantId, Long exceptId) {
        return databaseClient
                .sql("UPDATE " + TABLE + " SET default_flag = 0, operate_time = :operate_time"
                        + " WHERE tenant_id = :tenant_id AND default_flag = 1 AND deleted = 0 AND id <> :id")
                .bind("operate_time", utcNow())
                .bind("tenant_id", tenantId)
                .bind("id", exceptId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    private ModelConfigBO copyWithIdentity(ModelConfigBO source, RequestHeader.PrincipalHeader header, Long id) {
        if (source == null) throw new IllegalArgumentException("model config must not be null");
        source.setId(id);
        source.setTenantId(header.getTenantId());
        source.setRemark(source.getRemark() == null ? "" : source.getRemark());
        source.setCreatorId(source.getCreatorId() == null ? header.getUserId() : source.getCreatorId());
        source.setCreatorName(source.getCreatorName() == null ? header.getUserName() : source.getCreatorName());
        source.setOperatorId(header.getUserId());
        source.setOperatorName(header.getUserName());
        source.setCreateTime(
                source.getCreateTime() == null ? LocalDateTime.now(ZoneOffset.UTC) : source.getCreateTime());
        source.setOperateTime(LocalDateTime.now(ZoneOffset.UTC));
        return source;
    }

    private ModelConfigBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        ModelConfigBO value = new ModelConfigBO();
        value.setId(row.get("id", Long.class));
        value.setModel(row.get("model", String.class));
        value.setLabel(row.get("label", String.class));
        value.setProviderId(row.get("provider_id", Long.class));
        value.setProviderName(row.get("provider_name", String.class));
        value.setStream(row.get("stream", Boolean.class));
        value.setToolCall(row.get("tool_call", Boolean.class));
        value.setVision(row.get("vision", Boolean.class));
        value.setReasoning(row.get("reasoning", Boolean.class));
        value.setTemperature(row.get("temperature", Double.class));
        value.setMaxTokens(row.get("max_tokens", Integer.class));
        Number defaultFlag = row.get("default_flag", Number.class);
        value.setDefaultFlag(DefaultFlagEnum.ofIndex(defaultFlag == null ? null : defaultFlag.byteValue()));
        Number enableFlag = row.get("enable_flag", Number.class);
        value.setEnableFlag(EnableFlagEnum.ofIndex(enableFlag == null ? null : enableFlag.byteValue()));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setRemark(row.get("remark", String.class));
        value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class));
        value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(time(row.get("operate_time")));
        return value;
    }

    private Byte defaultFlag(ModelConfigBO value) {
        return value.getDefaultFlag() == null
                ? DefaultFlagEnum.NOT_DEFAULT.getIndex()
                : value.getDefaultFlag().getIndex();
    }

    private Byte enableFlag(ModelConfigBO value) {
        return value.getEnableFlag() == null
                ? EnableFlagEnum.ENABLE.getIndex()
                : value.getEnableFlag().getIndex();
    }

    private LocalDateTime utc(LocalDateTime value) {
        return value == null ? utcNow() : value;
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return null;
    }

    private void validateHeader(RequestHeader.PrincipalHeader header) {
        if (header == null || header.getTenantId() == null || header.getUserId() == null) {
            throw new IllegalArgumentException("tenant and user are required");
        }
    }
}
