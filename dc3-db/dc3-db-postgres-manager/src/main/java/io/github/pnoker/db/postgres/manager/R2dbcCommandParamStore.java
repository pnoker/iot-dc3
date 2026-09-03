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

import io.github.pnoker.common.manager.repository.CommandParamFilter;
import io.github.pnoker.common.manager.repository.ReactiveCommandParamStore;

import io.github.pnoker.common.entity.ext.CommandParamExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ParamDirectionTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
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
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/** Explicit SQL adapter for manager command parameters. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcCommandParamStore implements ReactiveCommandParamStore {
    private static final String TABLE = "dc3_manager.dc3_command_param";
    private static final String COLUMNS =
            "id, param_name, param_code, param_direction_flag, param_type_flag, required_flag, default_value, param_ext, command_id, enable_flag, tenant_id, remark, signature, version, creator_id, creator_name, create_time, operator_id, operator_name, operate_time";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final ObjectMapper objectMapper;
    private final R2dbcDialect dialect;

    @Override
    public Mono<CommandParamBO> get(Long tenantId, Long id) {
        if (!valid(tenantId) || !valid(id)) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Flux<CommandParamBO> listByCommandId(Long tenantId, Long commandId) {
        if (!valid(tenantId) || !valid(commandId)) return Flux.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND command_id=:command_id AND deleted=0 ORDER BY id ASC")
                .bind("tenant_id", tenantId)
                .bind("command_id", commandId)
                .map(this::map)
                .all();
    }

    @Override
    public Flux<CommandParamBO> listByIds(Long tenantId, Collection<Long> ids) {
        if (!valid(tenantId) || ids == null || ids.isEmpty()) return Flux.empty();
        List<Long> values = ids.stream().filter(this::valid).distinct().toList();
        if (values.isEmpty()) return Flux.empty();
        String placeholders = java.util.stream.IntStream.range(0, values.size())
                .mapToObj(index -> ":id" + index)
                .reduce((left, right) -> left + "," + right)
                .orElseThrow();
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND id IN (" + placeholders
                        + ") AND deleted=0 ORDER BY id ASC")
                .bind("tenant_id", tenantId);
        for (int index = 0; index < values.size(); index++) spec = spec.bind("id" + index, values.get(index));
        return spec.map(this::map).all();
    }

    @Override
    public Mono<Boolean> existsByNameOrCode(
            Long tenantId, Long commandId, String paramName, String paramCode, Long excludedId) {
        if (!valid(tenantId) || !valid(commandId) || (blank(paramName) && blank(paramCode))) return Mono.just(false);
        StringBuilder sql = new StringBuilder("SELECT 1 FROM ")
                .append(TABLE)
                .append(" WHERE tenant_id=:tenant_id AND command_id=:command_id AND deleted=0");
        if (!blank(paramName) && !blank(paramCode))
            sql.append(" AND (param_name=:param_name OR param_code=:param_code)");
        else if (!blank(paramName)) sql.append(" AND param_name=:param_name");
        else sql.append(" AND param_code=:param_code");
        if (valid(excludedId)) sql.append(" AND id<>:excluded_id");
        DatabaseClient.GenericExecuteSpec spec =
                databaseClient.sql(sql + " LIMIT 1").bind("tenant_id", tenantId).bind("command_id", commandId);
        if (!blank(paramName)) spec = spec.bind("param_name", paramName.trim());
        if (!blank(paramCode)) spec = spec.bind("param_code", paramCode.trim());
        if (valid(excludedId)) spec = spec.bind("excluded_id", excludedId);
        return spec.map((row, metadata) -> true).one().defaultIfEmpty(false);
    }

    @Override
    public Mono<CommandParamBO> insert(CommandParamBO value) {
        if (value == null
                || !valid(value.getTenantId())
                || !valid(value.getCommandId())
                || blank(value.getParamName())
                || blank(value.getParamCode())) {
            return Mono.error(
                    new IllegalArgumentException("tenantId, commandId, paramName and paramCode are required"));
        }
        if (value.getId() == null) value.setId(UuidV7.nextLong());
        if (value.getVersion() == null) value.setVersion(0);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(
                        "INSERT INTO " + TABLE + " (" + COLUMNS
                                + ", deleted) VALUES (:id,:param_name,:param_code,:param_direction_flag,:param_type_flag,:required_flag,:default_value,"
                                + dialect.jsonWriteExpression(":param_ext")
                                + ",:command_id,:enable_flag,:tenant_id,:remark,:signature,:version,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", value.getId())
                .bind("param_name", value.getParamName().trim())
                .bind("param_code", value.getParamCode().trim())
                .bind("param_direction_flag", index(value.getParamDirectionFlag()))
                .bind("param_type_flag", index(value.getParamTypeFlag()))
                .bind("required_flag", value.getRequiredFlag() != null && value.getRequiredFlag() ? (byte) 1 : (byte) 0)
                .bind("default_value", empty(value.getDefaultValue()))
                .bind("param_ext", serialize(value.getParamExt()))
                .bind("command_id", value.getCommandId())
                .bind("enable_flag", index(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("remark", empty(value.getRemark()))
                .bind("signature", empty(value.getSignature()))
                .bind("version", value.getVersion())
                .bind("creator_id", value.getCreatorId() == null ? 0L : value.getCreatorId())
                .bind("creator_name", empty(value.getCreatorName()))
                .bind("create_time", now)
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", empty(value.getOperatorName()))
                .bind("operate_time", now);
        return transactionalOperator
                .transactional(spec.fetch().rowsUpdated().then(get(value.getTenantId(), value.getId())))
                .onErrorMap(DataIntegrityViolationException.class, error -> error);
    }

    @Override
    public Mono<CommandParamBO> update(CommandParamBO value, int expectedVersion) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET param_name=:param_name, param_code=:param_code, param_direction_flag=:param_direction_flag, param_type_flag=:param_type_flag, required_flag=:required_flag, default_value=:default_value, param_ext="
                                + dialect.jsonWriteExpression(":param_ext")
                                + ", command_id=:command_id, enable_flag=:enable_flag, remark=:remark, signature=:signature, version=version+1, operator_id=:operator_id, operator_name=:operator_name, operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND version=:expected_version AND deleted=0")
                .bind("param_name", empty(value.getParamName()))
                .bind("param_code", empty(value.getParamCode()))
                .bind("param_direction_flag", index(value.getParamDirectionFlag()))
                .bind("param_type_flag", index(value.getParamTypeFlag()))
                .bind("required_flag", value.getRequiredFlag() != null && value.getRequiredFlag() ? (byte) 1 : (byte) 0)
                .bind("default_value", empty(value.getDefaultValue()))
                .bind("param_ext", serialize(value.getParamExt()))
                .bind("command_id", value.getCommandId())
                .bind("enable_flag", index(value.getEnableFlag()))
                .bind("remark", empty(value.getRemark()))
                .bind("signature", empty(value.getSignature()))
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", empty(value.getOperatorName()))
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .bind("tenant_id", value.getTenantId())
                .bind("id", value.getId())
                .bind("expected_version", expectedVersion);
        return transactionalOperator.transactional(spec.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (!valid(tenantId) || !valid(id)) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET deleted=1, operator_id=:operator_id, operator_name=:operator_name, operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND version=:expected_version AND deleted=0")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .bind("expected_version", expectedVersion)
                .bind("operator_id", operatorId == null ? 0L : operatorId)
                .bind("operator_name", empty(operatorName))
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1));
    }

    @Override
    public Mono<Long> deleteByCommandId(Long tenantId, Long commandId, Long operatorId, String operatorName) {
        if (!valid(tenantId) || !valid(commandId)) return Mono.just(0L);
        return transactionalOperator.transactional(databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET deleted=1, operator_id=:operator_id, operator_name=:operator_name, operate_time=:operate_time WHERE tenant_id=:tenant_id AND command_id=:command_id AND deleted=0")
                .bind("tenant_id", tenantId)
                .bind("command_id", commandId)
                .bind("operator_id", operatorId == null ? 0L : operatorId)
                .bind("operator_name", empty(operatorName))
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .fetch()
                .rowsUpdated());
    }

    @Override
    public Mono<OffsetPage<CommandParamBO>> list(CommandParamFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("filter is required"));
        StringBuilder where = new StringBuilder(" WHERE p.tenant_id=:tenant_id AND p.deleted=0");
        if (!blank(filter.paramName())) where.append(" AND p.param_name LIKE :param_name");
        if (!blank(filter.paramCode())) where.append(" AND p.param_code=:param_code");
        if (filter.paramDirection() != null) where.append(" AND p.param_direction_flag=:param_direction_flag");
        if (filter.paramTypeFlag() != null) where.append(" AND p.param_type_flag=:param_type_flag");
        if (filter.commandId() != null) where.append(" AND p.command_id=:command_id");
        if (filter.enableFlag() != null) where.append(" AND p.enable_flag=:enable_flag");
        if (filter.version() != null) where.append(" AND p.version=:version");
        DatabaseClient.GenericExecuteSpec count =
                bind(databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + " p" + where), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(
                        databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " p" + where
                                + orderBy(filter.sort()) + " LIMIT :limit OFFSET :offset"),
                        filter)
                .bind("limit", filter.limit())
                .bind("offset", filter.offset());
        Mono<Long> total = count.map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                })
                .one()
                .defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(this::map)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(items, filter.offset(), filter.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, CommandParamFilter filter) {
        spec = spec.bind("tenant_id", filter.tenantId());
        if (!blank(filter.paramName()))
            spec = spec.bind("param_name", "%" + filter.paramName().trim() + "%");
        if (!blank(filter.paramCode()))
            spec = spec.bind("param_code", filter.paramCode().trim());
        if (filter.paramDirection() != null)
            spec = spec.bind("param_direction_flag", filter.paramDirection().getIndex());
        if (filter.paramTypeFlag() != null)
            spec = spec.bind("param_type_flag", filter.paramTypeFlag().getIndex());
        if (filter.commandId() != null) spec = spec.bind("command_id", filter.commandId());
        if (filter.enableFlag() != null)
            spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        if (filter.version() != null) spec = spec.bind("version", filter.version());
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        List<String> clauses = new ArrayList<>();
        if (sort != null)
            for (SortSpec spec : sort) {
                String column =
                        switch (spec.field()) {
                            case "id" -> "p.id";
                            case "paramName" -> "p.param_name";
                            case "paramCode" -> "p.param_code";
                            case "commandId" -> "p.command_id";
                            case "createTime" -> "p.create_time";
                            case "operateTime" -> "p.operate_time";
                            case "version" -> "p.version";
                            default -> throw new IllegalArgumentException("unsupported command param sort field");
                        };
                clauses.add(column + (spec.direction() == SortSpec.Direction.DESC ? " DESC" : " ASC"));
            }
        if (clauses.stream().noneMatch(value -> value.startsWith("p.id"))) clauses.add("p.id ASC");
        return " ORDER BY " + String.join(", ", clauses);
    }

    private CommandParamBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        CommandParamBO value = new CommandParamBO();
        value.setId(row.get("id", Long.class));
        value.setParamName(row.get("param_name", String.class));
        value.setParamCode(row.get("param_code", String.class));
        value.setCommandId(row.get("command_id", Long.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setDefaultValue(row.get("default_value", String.class));
        value.setRemark(row.get("remark", String.class));
        value.setSignature(row.get("signature", String.class));
        value.setVersion(row.get("version", Integer.class));
        value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class));
        value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(time(row.get("operate_time")));
        Number direction = row.get("param_direction_flag", Number.class),
                type = row.get("param_type_flag", Number.class),
                required = row.get("required_flag", Number.class),
                enabled = row.get("enable_flag", Number.class);
        value.setParamDirectionFlag(ParamDirectionTypeEnum.ofIndex(direction == null ? null : direction.byteValue()));
        value.setParamTypeFlag(PointTypeEnum.ofIndex(type == null ? null : type.byteValue()));
        value.setRequiredFlag(required != null && required.byteValue() == 1);
        value.setEnableFlag(EnableFlagEnum.ofIndex(enabled == null ? null : enabled.byteValue()));
        String raw = row.get("param_ext", String.class);
        if (raw != null)
            try {
                JsonExt json = objectMapper.readValue(raw, JsonExt.class);
                CommandParamExt ext = new CommandParamExt();
                ext.setType(json.getType());
                ext.setVersion(json.getVersion());
                ext.setRemark(json.getRemark());
                ext.setContent(
                        json.getContent() == null
                                ? null
                                : JsonUtil.parseObject(json.getContent(), CommandParamExt.Content.class));
                value.setParamExt(ext);
            } catch (Exception exception) {
                throw new IllegalStateException("param_ext contains invalid JSON", exception);
            }
        return value;
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return null;
    }

    private Byte index(ParamDirectionTypeEnum value) {
        return value == null ? ParamDirectionTypeEnum.INPUT.getIndex() : value.getIndex();
    }

    private Byte index(PointTypeEnum value) {
        return value == null ? PointTypeEnum.STRING.getIndex() : value.getIndex();
    }

    private Byte index(EnableFlagEnum value) {
        return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex();
    }

    private String serialize(CommandParamExt value) {
        try {
            JsonExt json = new JsonExt();
            if (value != null) {
                json.setType(value.getType());
                json.setVersion(value.getVersion());
                json.setRemark(value.getRemark());
                json.setContent(value.getContent() == null ? null : JsonUtil.toJsonString(value.getContent()));
            }
            return objectMapper.writeValueAsString(json);
        } catch (Exception exception) {
            throw new IllegalArgumentException("param_ext is not valid JSON", exception);
        }
    }

    private String empty(String value) {
        return value == null ? "" : value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean valid(Long value) {
        return value != null && value > 0;
    }
}
