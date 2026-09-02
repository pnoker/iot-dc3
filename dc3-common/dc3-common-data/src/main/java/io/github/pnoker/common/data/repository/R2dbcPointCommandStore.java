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
package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.PointCommandHistoryDO;
import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.enums.PointCommandTypeEnum;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
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
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for point command history. */
@Repository
@ConditionalOnClass({DatabaseClient.class, PageTransaction.class})
@RequiredArgsConstructor
public class R2dbcPointCommandStore implements ReactivePointCommandStore {

    private static final String TABLE = "dc3_data.dc3_point_command_history";
    private static final String COLUMNS =
            "id, command_id, tenant_id, type, device_id, point_id, request_value, response_value,"
                    + " status, error_code, error_message, source, source_user_id, occur_time, send_time, finish_time, expire_time,"
                    + " schema_version, create_time, operate_time";

    private final DatabaseClient databaseClient;
    private final PageTransaction pageTransaction;

    @Override
    public Mono<PointCommandHistoryDO> find(Long tenantId, String commandId) {
        if (tenantId == null || commandId == null || commandId.isBlank()) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id = :tenant_id AND command_id = :command_id LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("command_id", commandId)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<PointCommandHistoryDO> insert(PointCommandHistoryDO command) {
        if (command == null
                || command.getTenantId() == null
                || command.getCommandId() == null
                || command.getCommandId().isBlank()) {
            return Mono.error(new IllegalArgumentException("tenantId and commandId are required"));
        }
        if (command.getId() == null) command.setId(UuidV7.nextLong());
        LocalDateTime now = utcNow();
        if (command.getOccurTime() == null) command.setOccurTime(now);
        if (command.getCreateTime() == null) command.setCreateTime(now);
        if (command.getOperateTime() == null) command.setOperateTime(now);
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES "
                        + "(:id, :command_id, :tenant_id, :type, :device_id, :point_id, :request_value, :response_value,"
                        + " :status, :error_code, :error_message, :source, :source_user_id, :occur_time, :send_time,"
                        + " :finish_time, :expire_time, :schema_version, :create_time, :operate_time)")
                .bind("id", command.getId())
                .bind("command_id", command.getCommandId())
                .bind("tenant_id", command.getTenantId())
                .bind("type", index(command.getType()))
                .bind("device_id", command.getDeviceId())
                .bind("point_id", command.getPointId())
                .bind("status", index(command.getStatus()))
                .bind("source", index(command.getSource()))
                .bind("occur_time", command.getOccurTime());
        spec = bindNullable(spec, "expire_time", command.getExpireTime(), LocalDateTime.class);
        spec = spec.bind("schema_version", command.getSchemaVersion() == null ? (short) 1 : command.getSchemaVersion())
                .bind("create_time", command.getCreateTime())
                .bind("operate_time", command.getOperateTime());
        spec = bindNullable(spec, "request_value", command.getRequestValue(), String.class);
        spec = bindNullable(spec, "response_value", command.getResponseValue(), String.class);
        spec = bindNullable(spec, "error_code", command.getErrorCode(), String.class);
        spec = bindNullable(spec, "error_message", command.getErrorMessage(), String.class);
        spec = bindNullable(spec, "source_user_id", command.getSourceUserId(), Long.class);
        spec = bindNullable(spec, "send_time", command.getSendTime(), LocalDateTime.class);
        spec = bindNullable(spec, "finish_time", command.getFinishTime(), LocalDateTime.class);
        return spec.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? find(command.getTenantId(), command.getCommandId())
                        : Mono.error(new IllegalStateException("point command insert affected " + rows + " rows")));
    }

    @Override
    public Mono<Boolean> markSent(Long tenantId, String commandId, Instant sentAt) {
        return update(
                tenantId,
                commandId,
                "status = :status, send_time = :send_time",
                spec -> spec.bind("status", PointCommandStatusEnum.SENT.getIndex())
                        .bind("send_time", local(sentAt)));
    }

    @Override
    public Mono<Boolean> markPublishFailed(
            Long tenantId, String commandId, String errorCode, String errorMessage, Instant finishedAt) {
        return update(
                tenantId,
                commandId,
                "status = :status, error_code = :error_code, error_message = :error_message, finish_time = :finish_time",
                spec -> {
                    spec = spec.bind("status", PointCommandStatusEnum.FAILED.getIndex())
                            .bind("finish_time", local(finishedAt));
                    spec = bindNullable(spec, "error_code", errorCode, String.class);
                    return bindNullable(spec, "error_message", errorMessage, String.class);
                });
    }

    @Override
    public Mono<Boolean> complete(
            Long tenantId,
            String commandId,
            PointCommandStatusEnum status,
            String responseValue,
            String errorCode,
            String errorMessage,
            Instant finishedAt) {
        return update(
                tenantId,
                commandId,
                "status = :status, response_value = :response_value, error_code = :error_code, error_message = :error_message, finish_time = :finish_time",
                spec -> {
                    spec = spec.bind("status", index(status)).bind("finish_time", local(finishedAt));
                    spec = bindNullable(spec, "response_value", responseValue, String.class);
                    spec = bindNullable(spec, "error_code", errorCode, String.class);
                    return bindNullable(spec, "error_message", errorMessage, String.class);
                });
    }

    @Override
    public Mono<Boolean> markDead(
            Long tenantId, String commandId, String errorCode, String errorMessage, Instant finishedAt) {
        return update(
                tenantId,
                commandId,
                "status = :status, error_code = :error_code, error_message = :error_message, finish_time = :finish_time",
                spec -> {
                    spec = spec.bind("status", PointCommandStatusEnum.DEAD.getIndex())
                            .bind("finish_time", local(finishedAt));
                    spec = bindNullable(spec, "error_code", errorCode, String.class);
                    return bindNullable(spec, "error_message", errorMessage, String.class);
                });
    }

    @Override
    public Mono<OffsetPage<PointCommandHistoryDO>> list(
            Long tenantId,
            Long deviceId,
            Long pointId,
            PointCommandStatusEnum status,
            PointCommandTypeEnum type,
            long offset,
            int limit,
            List<SortSpec> sort) {
        if (tenantId == null) return Mono.error(new IllegalArgumentException("tenantId is required"));
        if (offset < 0 || limit < 1 || limit > PageRequest.MAX_LIMIT) {
            return Mono.error(new IllegalArgumentException("invalid page bounds"));
        }
        List<String> predicates = new ArrayList<>();
        predicates.add("tenant_id = :tenant_id");
        if (deviceId != null) predicates.add("device_id = :device_id");
        if (pointId != null) predicates.add("point_id = :point_id");
        if (status != null) predicates.add("status = :status");
        if (type != null) predicates.add("type = :type");
        String where = " WHERE " + String.join(" AND ", predicates);
        DatabaseClient.GenericExecuteSpec count = bindFilters(
                databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + where),
                tenantId,
                deviceId,
                pointId,
                status,
                type);
        DatabaseClient.GenericExecuteSpec rows = bindFilters(
                        databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + where + " ORDER BY " + orderBy(sort)
                                + " LIMIT :limit OFFSET :offset"),
                        tenantId,
                        deviceId,
                        pointId,
                        status,
                        type)
                .bind("limit", limit)
                .bind("offset", offset);
        Mono<Long> total = count.map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                })
                .one()
                .defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(this::map)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(items, offset, limit, totalCount)))
                .as(pageTransaction::transactional);
    }

    private Mono<Boolean> update(
            Long tenantId,
            String commandId,
            String assignments,
            java.util.function.UnaryOperator<DatabaseClient.GenericExecuteSpec> binder) {
        if (tenantId == null || commandId == null || commandId.isBlank()) return Mono.just(false);
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("UPDATE " + TABLE + " SET " + assignments
                        + " WHERE tenant_id = :tenant_id AND command_id = :command_id")
                .bind("tenant_id", tenantId)
                .bind("command_id", commandId);
        return binder.apply(spec).fetch().rowsUpdated().map(rows -> rows == 1);
    }

    private DatabaseClient.GenericExecuteSpec bindFilters(
            DatabaseClient.GenericExecuteSpec spec,
            Long tenantId,
            Long deviceId,
            Long pointId,
            PointCommandStatusEnum status,
            PointCommandTypeEnum type) {
        spec = spec.bind("tenant_id", tenantId);
        if (deviceId != null) spec = spec.bind("device_id", deviceId);
        if (pointId != null) spec = spec.bind("point_id", pointId);
        if (status != null) spec = spec.bind("status", status.getIndex());
        if (type != null) spec = spec.bind("type", type.getIndex());
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "occur_time DESC, id DESC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column =
                    switch (spec.field()) {
                        case "occurTime" -> "occur_time";
                        case "sendTime" -> "send_time";
                        case "finishTime" -> "finish_time";
                        case "status" -> "status";
                        case "createTime" -> "create_time";
                        default -> throw new IllegalArgumentException("unsupported sort field: " + spec.field());
                    };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(clause -> clause.startsWith("id "))) clauses.add("id DESC");
        return String.join(", ", clauses);
    }

    private PointCommandHistoryDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        PointCommandHistoryDO value = new PointCommandHistoryDO();
        value.setId(row.get("id", Long.class));
        value.setCommandId(row.get("command_id", String.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setDeviceId(row.get("device_id", Long.class));
        value.setPointId(row.get("point_id", Long.class));
        value.setRequestValue(row.get("request_value", String.class));
        value.setResponseValue(row.get("response_value", String.class));
        value.setErrorCode(row.get("error_code", String.class));
        value.setErrorMessage(row.get("error_message", String.class));
        value.setSourceUserId(row.get("source_user_id", Long.class));
        value.setSchemaVersion(row.get("schema_version", Short.class));
        value.setType(enumType(row.get("type", Number.class)));
        value.setStatus(enumStatus(row.get("status", Number.class)));
        value.setSource(enumSource(row.get("source", Number.class)));
        value.setOccurTime(time(row.get("occur_time")));
        value.setSendTime(time(row.get("send_time")));
        value.setFinishTime(time(row.get("finish_time")));
        value.setExpireTime(time(row.get("expire_time")));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperateTime(time(row.get("operate_time")));
        return value;
    }

    private Byte index(PointCommandTypeEnum value) {
        return value == null ? PointCommandTypeEnum.READ.getIndex() : value.getIndex();
    }

    private Byte index(PointCommandStatusEnum value) {
        return value == null ? PointCommandStatusEnum.PENDING.getIndex() : value.getIndex();
    }

    private Byte index(PointCommandSourceEnum value) {
        return value == null ? PointCommandSourceEnum.HTTP.getIndex() : value.getIndex();
    }

    private PointCommandTypeEnum enumType(Number value) {
        return PointCommandTypeEnum.ofIndex(value == null ? null : value.byteValue());
    }

    private PointCommandStatusEnum enumStatus(Number value) {
        return PointCommandStatusEnum.ofIndex(value == null ? null : value.byteValue());
    }

    private PointCommandSourceEnum enumSource(Number value) {
        return PointCommandSourceEnum.ofIndex(value == null ? null : value.byteValue());
    }

    private LocalDateTime local(Instant value) {
        return LocalDateTime.ofInstant(value == null ? Instant.now() : value, ZoneOffset.UTC);
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

    private <T> DatabaseClient.GenericExecuteSpec bindNullable(
            DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }
}
