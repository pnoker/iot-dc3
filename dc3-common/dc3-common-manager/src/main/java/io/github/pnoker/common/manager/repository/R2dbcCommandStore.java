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

import io.github.pnoker.common.entity.ext.CommandExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.CallTypeEnum;
import io.github.pnoker.common.enums.CommandTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
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
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/** Explicit SQL adapter for manager commands. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcCommandStore implements ReactiveCommandStore {
    private static final String TABLE = "dc3_manager.dc3_command";
    private static final String COLUMNS =
            "id, command_name, command_code, command_type_flag, call_type_flag, timeout, command_ext, profile_id, enable_flag, tenant_id, remark, signature, version, creator_id, creator_name, create_time, operator_id, operator_name, operate_time";
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final ObjectMapper objectMapper;
    private final R2dbcDialect dialect;

    @Override
    public Mono<CommandBO> get(Long tenantId, Long id) {
        if (tenantId == null || id == null) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<Boolean> existsByNameOrCode(
            Long tenantId, Long profileId, String commandName, String commandCode, Long excludingId) {
        if (tenantId == null || profileId == null || (commandName == null && commandCode == null))
            return Mono.just(false);
        StringBuilder sql = new StringBuilder("SELECT 1 FROM ")
                .append(TABLE)
                .append(" WHERE tenant_id=:tenant_id AND profile_id=:profile_id AND deleted=0 AND (");
        List<String> predicates = new ArrayList<>();
        if (commandName != null && !commandName.isBlank()) predicates.add("command_name=:command_name");
        if (commandCode != null && !commandCode.isBlank()) predicates.add("command_code=:command_code");
        if (predicates.isEmpty()) return Mono.just(false);
        sql.append(String.join(" OR ", predicates)).append(")");
        if (excludingId != null) sql.append(" AND id<>:excluding_id");
        DatabaseClient.GenericExecuteSpec query =
                databaseClient.sql(sql + " LIMIT 1").bind("tenant_id", tenantId).bind("profile_id", profileId);
        if (commandName != null && !commandName.isBlank()) query = query.bind("command_name", commandName);
        if (commandCode != null && !commandCode.isBlank()) query = query.bind("command_code", commandCode);
        if (excludingId != null) query = query.bind("excluding_id", excludingId);
        return query.map((row, metadata) -> true).one().defaultIfEmpty(false);
    }

    @Override
    public Mono<CommandBO> insert(CommandBO value) {
        if (value == null || value.getTenantId() == null || value.getProfileId() == null) {
            return Mono.error(new IllegalArgumentException("tenantId and profileId are required"));
        }
        if (value.getId() == null) value.setId(UuidV7.nextLong());
        if (value.getVersion() == null) value.setVersion(0);
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(
                        "INSERT INTO " + TABLE
                                + " (id, command_name, command_code, command_type_flag, call_type_flag, timeout, command_ext, profile_id, enable_flag, tenant_id, remark, signature, version, creator_id, creator_name, create_time, operator_id, operator_name, operate_time, deleted)"
                                + " VALUES (:id,:command_name,:command_code,:command_type,:call_type,:timeout,"
                                + dialect.jsonWriteExpression(":command_ext")
                                + ",:profile_id,:enable_flag,:tenant_id,:remark,:signature,:version,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", value.getId())
                .bind("command_name", value.getCommandName())
                .bind("command_code", value.getCommandCode())
                .bind("command_type", index(value.getCommandTypeFlag()))
                .bind("call_type", index(value.getCallTypeFlag()))
                .bind("timeout", value.getTimeout())
                .bind("command_ext", serialize(value.getCommandExt()))
                .bind("profile_id", value.getProfileId())
                .bind("enable_flag", index(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("signature", value.getSignature() == null ? "" : value.getSignature())
                .bind("version", value.getVersion())
                .bind("creator_id", value.getCreatorId() == null ? 0L : value.getCreatorId())
                .bind("creator_name", value.getCreatorName() == null ? "" : value.getCreatorName())
                .bind(
                        "create_time",
                        value.getCreateTime() == null ? LocalDateTime.now(ZoneOffset.UTC) : value.getCreateTime())
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", value.getOperatorName() == null ? "" : value.getOperatorName())
                .bind(
                        "operate_time",
                        value.getOperateTime() == null ? LocalDateTime.now(ZoneOffset.UTC) : value.getOperateTime());
        return transactionalOperator.transactional(query.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? get(value.getTenantId(), value.getId())
                        : Mono.error(new IllegalStateException("command insert affected " + rows + " rows"))));
    }

    @Override
    public Mono<CommandBO> update(CommandBO value, int expectedVersion) {
        if (value == null || value.getId() == null || value.getTenantId() == null) {
            return Mono.error(new IllegalArgumentException("tenantId and command id are required"));
        }
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET command_name=:command_name, command_type_flag=:command_type, call_type_flag=:call_type, timeout=:timeout, command_ext="
                                + dialect.jsonWriteExpression(":command_ext")
                                + ", profile_id=:profile_id, enable_flag=:enable_flag, remark=:remark, signature=:signature, version=version+1, operator_id=:operator_id, operator_name=:operator_name, operate_time=:operate_time WHERE id=:id AND tenant_id=:tenant_id AND version=:expected_version AND deleted=0")
                .bind("command_name", value.getCommandName())
                .bind("command_type", index(value.getCommandTypeFlag()))
                .bind("call_type", index(value.getCallTypeFlag()))
                .bind("timeout", value.getTimeout())
                .bind("command_ext", serialize(value.getCommandExt()))
                .bind("profile_id", value.getProfileId())
                .bind("enable_flag", index(value.getEnableFlag()))
                .bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("signature", value.getSignature() == null ? "" : value.getSignature())
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", value.getOperatorName() == null ? "" : value.getOperatorName())
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .bind("id", value.getId())
                .bind("tenant_id", value.getTenantId())
                .bind("expected_version", expectedVersion);
        return transactionalOperator.transactional(query.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET deleted=1, operator_id=:operator_id, operator_name=:operator_name, operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND version=:expected_version AND deleted=0")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .bind("expected_version", expectedVersion)
                .bind("operator_id", operatorId == null ? 0L : operatorId)
                .bind("operator_name", operatorName == null ? "" : operatorName)
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1));
    }

    @Override
    public Flux<CommandBO> listByIds(Long tenantId, List<Long> ids) {
        if (tenantId == null || ids == null || ids.isEmpty()) return Flux.empty();
        List<Long> values =
                ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (values.isEmpty()) return Flux.empty();
        String placeholders = java.util.stream.IntStream.range(0, values.size())
                .mapToObj(i -> ":id" + i)
                .reduce((a, b) -> a + "," + b)
                .orElseThrow();
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND deleted=0 AND id IN ("
                        + placeholders + ") ORDER BY id")
                .bind("tenant_id", tenantId);
        for (int i = 0; i < values.size(); i++) spec = spec.bind("id" + i, values.get(i));
        return spec.map(this::map).all();
    }

    @Override
    public Flux<CommandBO> listByProfileId(Long tenantId, Long profileId) {
        if (tenantId == null || profileId == null) return Flux.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND profile_id=:profile_id AND deleted=0 ORDER BY id")
                .bind("tenant_id", tenantId)
                .bind("profile_id", profileId)
                .map(this::map)
                .all();
    }

    @Override
    public Flux<CommandBO> listByDeviceId(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Flux.empty();
        return databaseClient
                .sql(
                        "SELECT " + qualifiedColumns("c") + " FROM " + TABLE
                                + " c JOIN dc3_manager.dc3_device d ON d.tenant_id=c.tenant_id AND d.profile_id=c.profile_id WHERE d.tenant_id=:tenant_id AND d.id=:device_id AND d.deleted=0 AND c.deleted=0 ORDER BY c.id")
                .bind("tenant_id", tenantId)
                .bind("device_id", deviceId)
                .map(this::map)
                .all();
    }

    @Override
    public Mono<OffsetPage<CommandBO>> list(CommandFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("filter is required"));
        StringBuilder where = new StringBuilder(" WHERE c.tenant_id=:tenant_id AND c.deleted=0");
        if (present(filter.commandName())) where.append(" AND c.command_name LIKE :command_name");
        if (present(filter.commandCode())) where.append(" AND c.command_code=:command_code");
        if (filter.commandTypeFlag() != null) where.append(" AND c.command_type_flag=:command_type");
        if (filter.callTypeFlag() != null) where.append(" AND c.call_type_flag=:call_type");
        if (filter.profileId() != null) where.append(" AND c.profile_id=:profile_id");
        if (filter.enableFlag() != null) where.append(" AND c.enable_flag=:enable_flag");
        if (filter.version() != null) where.append(" AND c.version=:version");
        if (filter.deviceId() != null)
            where.append(
                    " AND EXISTS (SELECT 1 FROM dc3_manager.dc3_device d WHERE d.tenant_id=c.tenant_id AND d.id=:device_id AND d.profile_id=c.profile_id AND d.deleted=0)");
        String predicate = where.toString();
        DatabaseClient.GenericExecuteSpec count =
                bind(databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + " c" + predicate), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(
                        databaseClient.sql("SELECT " + qualifiedColumns("c") + " FROM " + TABLE + " c" + predicate
                                + " ORDER BY " + orderBy(filter.sort()) + " LIMIT :limit OFFSET :offset"),
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

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, CommandFilter filter) {
        spec = spec.bind("tenant_id", filter.tenantId());
        if (present(filter.commandName())) spec = spec.bind("command_name", "%" + filter.commandName() + "%");
        if (present(filter.commandCode())) spec = spec.bind("command_code", filter.commandCode());
        if (filter.commandTypeFlag() != null)
            spec = spec.bind("command_type", filter.commandTypeFlag().getIndex());
        if (filter.callTypeFlag() != null)
            spec = spec.bind("call_type", filter.callTypeFlag().getIndex());
        if (filter.profileId() != null) spec = spec.bind("profile_id", filter.profileId());
        if (filter.enableFlag() != null)
            spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        if (filter.version() != null) spec = spec.bind("version", filter.version());
        if (filter.deviceId() != null) spec = spec.bind("device_id", filter.deviceId());
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "c.id ASC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column =
                    switch (spec.field()) {
                        case "id" -> "c.id";
                        case "commandName" -> "c.command_name";
                        case "commandCode" -> "c.command_code";
                        case "createTime" -> "c.create_time";
                        case "operateTime" -> "c.operate_time";
                        case "version" -> "c.version";
                        default ->
                            throw new IllegalArgumentException("unsupported command sort field: " + spec.field());
                    };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("c.id "))) clauses.add("c.id ASC");
        return String.join(", ", clauses);
    }

    private String qualifiedColumns(String alias) {
        return java.util.Arrays.stream(COLUMNS.split(", "))
                .map(column -> alias + "." + column)
                .reduce((a, b) -> a + ", " + b)
                .orElse(COLUMNS);
    }

    private CommandBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        CommandBO value = new CommandBO();
        value.setId(row.get("id", Long.class));
        value.setCommandName(row.get("command_name", String.class));
        value.setCommandCode(row.get("command_code", String.class));
        value.setTimeout(row.get("timeout", Integer.class));
        value.setProfileId(row.get("profile_id", Long.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setRemark(row.get("remark", String.class));
        value.setSignature(row.get("signature", String.class));
        value.setVersion(row.get("version", Integer.class));
        value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class));
        value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(time(row.get("operate_time")));
        Number commandType = row.get("command_type_flag", Number.class),
                callType = row.get("call_type_flag", Number.class),
                enabled = row.get("enable_flag", Number.class);
        value.setCommandTypeFlag(CommandTypeEnum.ofIndex(commandType == null ? null : commandType.byteValue()));
        value.setCallTypeFlag(CallTypeEnum.ofIndex(callType == null ? null : callType.byteValue()));
        value.setEnableFlag(EnableFlagEnum.ofIndex(enabled == null ? null : enabled.byteValue()));
        String raw = row.get("command_ext", String.class);
        if (raw != null)
            try {
                JsonExt json = objectMapper.readValue(raw, JsonExt.class);
                CommandExt ext = new CommandExt();
                ext.setType(json.getType());
                ext.setVersion(json.getVersion());
                ext.setRemark(json.getRemark());
                ext.setContent(
                        json.getContent() == null
                                ? null
                                : JsonUtil.parseObject(json.getContent(), CommandExt.Content.class));
                value.setCommandExt(ext);
            } catch (Exception exception) {
                throw new IllegalStateException("command_ext contains invalid JSON", exception);
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

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private Byte index(CommandTypeEnum value) {
        return value == null ? CommandTypeEnum.CUSTOM.getIndex() : value.getIndex();
    }

    private Byte index(CallTypeEnum value) {
        return value == null ? CallTypeEnum.SYNC.getIndex() : value.getIndex();
    }

    private Byte index(EnableFlagEnum value) {
        return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex();
    }

    private String serialize(CommandExt value) {
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
            throw new IllegalArgumentException("command_ext is not valid JSON", exception);
        }
    }
}
