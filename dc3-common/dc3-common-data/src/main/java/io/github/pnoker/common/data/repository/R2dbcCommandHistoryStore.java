package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.enums.CommandHistorySourceEnum;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Explicit SQL adapter for custom command history. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcCommandHistoryStore implements ReactiveCommandHistoryStore {

    private static final String TABLE = "dc3_data.dc3_command_history";
    private static final String COLUMNS = "id, record_id, tenant_id, device_id, command_id, command_code,"
            + " param_values, result_values, config_snapshot, status, error_code, error_message, source,"
            + " source_user_id, occur_time, send_time, finish_time, expire_time, schema_version, create_time, operate_time";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<CommandHistoryDO> find(Long tenantId, String recordId) {
        if (tenantId == null || recordId == null || recordId.isBlank()) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND record_id=:record_id LIMIT 1")
                .bind("tenant_id", tenantId).bind("record_id", recordId)
                .map(this::map).one();
    }

    @Override
    public Mono<CommandHistoryDO> insert(CommandHistoryDO history) {
        if (history == null || history.getTenantId() == null || history.getRecordId() == null
                || history.getRecordId().isBlank()) {
            return Mono.error(new IllegalArgumentException("tenantId and recordId are required"));
        }
        if (history.getId() == null) history.setId(UuidV7.nextLong());
        LocalDateTime now = utcNow();
        if (history.getOccurTime() == null) history.setOccurTime(now);
        if (history.getCreateTime() == null) history.setCreateTime(now);
        if (history.getOperateTime() == null) history.setOperateTime(now);
        String sql = "INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES "
                + "(:id,:record_id,:tenant_id,:device_id,:command_id,:command_code,"
                + dialect.jsonWriteExpression(":param_values") + ","
                + dialect.jsonWriteExpression(":result_values") + ","
                + dialect.jsonWriteExpression(":config_snapshot") + ",:status,:error_code,:error_message,:source,"
                + ":source_user_id,:occur_time,:send_time,:finish_time,:expire_time,:schema_version,:create_time,:operate_time)";
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql)
                .bind("id", history.getId()).bind("record_id", history.getRecordId())
                .bind("tenant_id", history.getTenantId()).bind("device_id", history.getDeviceId())
                .bind("command_id", history.getCommandId()).bind("command_code", history.getCommandCode())
                .bind("status", status(history.getStatus())).bind("source", source(history.getSource()))
                .bind("occur_time", history.getOccurTime()).bind("schema_version",
                        history.getSchemaVersion() == null ? (short) 1 : history.getSchemaVersion())
                .bind("create_time", history.getCreateTime()).bind("operate_time", history.getOperateTime());
        spec = bindNullable(spec, "param_values", history.getParamValues(), String.class);
        spec = bindNullable(spec, "result_values", history.getResultValues(), String.class);
        spec = bindNullable(spec, "config_snapshot", history.getConfigSnapshot(), String.class);
        spec = bindNullable(spec, "error_code", history.getErrorCode(), String.class);
        spec = bindNullable(spec, "error_message", history.getErrorMessage(), String.class);
        spec = bindNullable(spec, "source_user_id", history.getSourceUserId(), Long.class);
        spec = bindNullable(spec, "send_time", history.getSendTime(), LocalDateTime.class);
        spec = bindNullable(spec, "finish_time", history.getFinishTime(), LocalDateTime.class);
        spec = bindNullable(spec, "expire_time", history.getExpireTime(), LocalDateTime.class);
        return transactionalOperator.transactional(spec.fetch().rowsUpdated().flatMap(rows -> rows == 1
                ? find(history.getTenantId(), history.getRecordId())
                : Mono.error(new IllegalStateException("command history insert affected " + rows + " rows"))));
    }

    @Override
    public Mono<Boolean> markSent(Long tenantId, String recordId, Instant sentAt) {
        return update(tenantId, recordId,
                "status=:sent, send_time=:sent_time, operate_time=:operate_time",
                spec -> spec.bind("sent", PointCommandStatusEnum.SENT.getIndex())
                        .bind("sent_time", local(sentAt)).bind("operate_time", utcNow()),
                "status=:pending");
    }

    @Override
    public Mono<Boolean> markPublishFailed(Long tenantId, String recordId, String errorCode,
                                            String errorMessage, Instant finishedAt) {
        return update(tenantId, recordId,
                "status=:failed, error_code=:error_code, error_message=:error_message, finish_time=:finish_time, operate_time=:operate_time",
                spec -> bindNullable(spec.bind("failed", PointCommandStatusEnum.FAILED.getIndex()), "error_code", errorCode, String.class)
                        .bind("error_message", errorMessage == null ? "" : errorMessage)
                        .bind("finish_time", local(finishedAt)).bind("operate_time", utcNow()),
                "status IN (:pending,:sent)", true);
    }

    @Override
    public Mono<Boolean> complete(Long tenantId, String recordId, PointCommandStatusEnum status,
                                  String resultValues, String configSnapshot, String errorCode,
                                  String errorMessage, Instant finishedAt) {
        if (status == null) return Mono.error(new IllegalArgumentException("status is required"));
        return update(tenantId, recordId,
                "status=:status, result_values=" + dialect.jsonWriteExpression(":result_values")
                        + ", config_snapshot=" + dialect.jsonWriteExpression(":config_snapshot")
                        + ", error_code=:error_code, error_message=:error_message, finish_time=:finish_time, operate_time=:operate_time",
                spec -> {
                    DatabaseClient.GenericExecuteSpec bound = spec.bind("status", status.getIndex())
                            .bind("finish_time", local(finishedAt)).bind("operate_time", utcNow());
                    bound = bindNullable(bound, "result_values", resultValues, String.class);
                    bound = bindNullable(bound, "config_snapshot", configSnapshot, String.class);
                    bound = bindNullable(bound, "error_code", errorCode, String.class);
                    return bindNullable(bound, "error_message", errorMessage, String.class);
                }, "status IN (:pending,:sent)", true);
    }

    @Override
    public Mono<Boolean> markDead(Long tenantId, String recordId, String errorCode,
                                  String errorMessage, Instant finishedAt) {
        return update(tenantId, recordId,
                "status=:dead, error_code=:error_code, error_message=:error_message, finish_time=:finish_time, operate_time=:operate_time",
                spec -> bindNullable(spec.bind("dead", PointCommandStatusEnum.DEAD.getIndex()), "error_code", errorCode, String.class)
                        .bind("error_message", errorMessage == null ? "" : errorMessage)
                        .bind("finish_time", local(finishedAt)).bind("operate_time", utcNow()),
                "status IN (:pending,:sent)", true);
    }

    @Override
    public Mono<OffsetPage<CommandHistoryDO>> list(Long tenantId, Long deviceId, Long commandId,
                                                   String commandCode, PointCommandStatusEnum status,
                                                   long offset, int limit, List<SortSpec> sort) {
        if (tenantId == null || tenantId <= 0) return Mono.just(OffsetPage.of(List.of(), 0, PageRequest.DEFAULT_LIMIT, 0));
        PageRequest page = new PageRequest(offset, limit);
        StringBuilder where = new StringBuilder(" tenant_id=:tenant_id");
        if (deviceId != null) where.append(" AND device_id=:device_id");
        if (commandId != null) where.append(" AND command_id=:command_id");
        if (commandCode != null && !commandCode.isBlank()) where.append(" AND command_code=:command_code");
        if (status != null) where.append(" AND status=:status");
        String base = " FROM " + TABLE + " WHERE" + where;
        DatabaseClient.GenericExecuteSpec count = bindFilters(databaseClient.sql("SELECT COUNT(*) AS total" + base),
                tenantId, deviceId, commandId, commandCode, status);
        DatabaseClient.GenericExecuteSpec rows = bindFilters(databaseClient.sql("SELECT " + COLUMNS + base
                        + " ORDER BY " + orderBy(sort) + " LIMIT :limit OFFSET :offset"), tenantId, deviceId,
                commandId, commandCode, status).bind("limit", page.limit()).bind("offset", page.offset());
        Mono<Long> total = count.map((row, metadata) -> {
            Number value = row.get("total", Number.class);
            return value == null ? 0L : value.longValue();
        }).one().defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(this::map).all().collectList()
                        .map(items -> OffsetPage.of(items, page.offset(), page.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    private Mono<Boolean> update(Long tenantId, String recordId, String assignments,
                                 java.util.function.Function<DatabaseClient.GenericExecuteSpec, DatabaseClient.GenericExecuteSpec> binder,
                                 String state, boolean bindStates) {
        if (tenantId == null || recordId == null || recordId.isBlank()) return Mono.just(false);
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("UPDATE " + TABLE + " SET " + assignments
                        + " WHERE tenant_id=:tenant_id AND record_id=:record_id AND " + state)
                .bind("tenant_id", tenantId).bind("record_id", recordId);
        if (bindStates) spec = spec.bind("pending", PointCommandStatusEnum.PENDING.getIndex())
                .bind("sent", PointCommandStatusEnum.SENT.getIndex());
        return transactionalOperator.transactional(binder.apply(spec).fetch().rowsUpdated().map(rows -> rows == 1));
    }

    private Mono<Boolean> update(Long tenantId, String recordId, String assignments,
                                 java.util.function.Function<DatabaseClient.GenericExecuteSpec, DatabaseClient.GenericExecuteSpec> binder,
                                 String state) {
        return update(tenantId, recordId, assignments, binder, state, false);
    }

    private DatabaseClient.GenericExecuteSpec bindFilters(DatabaseClient.GenericExecuteSpec spec, Long tenantId,
                                                          Long deviceId, Long commandId, String commandCode,
                                                          PointCommandStatusEnum status) {
        spec = spec.bind("tenant_id", tenantId);
        if (deviceId != null) spec = spec.bind("device_id", deviceId);
        if (commandId != null) spec = spec.bind("command_id", commandId);
        if (commandCode != null && !commandCode.isBlank()) spec = spec.bind("command_code", commandCode);
        if (status != null) spec = spec.bind("status", status.getIndex());
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "occur_time DESC, id DESC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column = switch (spec.field()) {
                case "id" -> "id";
                case "occurTime" -> "occur_time";
                case "sendTime" -> "send_time";
                case "finishTime" -> "finish_time";
                case "status" -> "status";
                case "createTime" -> "create_time";
                default -> throw new IllegalArgumentException("unsupported sort field: " + spec.field());
            };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("id "))) clauses.add("id DESC");
        return String.join(", ", clauses);
    }

    private CommandHistoryDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        CommandHistoryDO value = new CommandHistoryDO();
        value.setId(row.get("id", Long.class)); value.setRecordId(row.get("record_id", String.class));
        value.setTenantId(row.get("tenant_id", Long.class)); value.setDeviceId(row.get("device_id", Long.class));
        value.setCommandId(row.get("command_id", Long.class)); value.setCommandCode(row.get("command_code", String.class));
        value.setParamValues(text(row.get("param_values", String.class))); value.setResultValues(text(row.get("result_values", String.class)));
        value.setConfigSnapshot(text(row.get("config_snapshot", String.class))); value.setErrorCode(row.get("error_code", String.class));
        value.setErrorMessage(row.get("error_message", String.class)); value.setSourceUserId(row.get("source_user_id", Long.class));
        Number status = row.get("status", Number.class), source = row.get("source", Number.class);
        value.setStatus(PointCommandStatusEnum.ofIndex(status == null ? null : status.byteValue()));
        value.setSource(CommandHistorySourceEnum.ofIndex(source == null ? null : source.byteValue()));
        value.setOccurTime(time(row.get("occur_time"))); value.setSendTime(time(row.get("send_time")));
        value.setFinishTime(time(row.get("finish_time"))); value.setExpireTime(time(row.get("expire_time")));
        value.setSchemaVersion(row.get("schema_version", Short.class)); value.setCreateTime(time(row.get("create_time")));
        value.setOperateTime(time(row.get("operate_time")));
        return value;
    }

    private Byte status(PointCommandStatusEnum value) { return value == null ? PointCommandStatusEnum.PENDING.getIndex() : value.getIndex(); }
    private Byte source(CommandHistorySourceEnum value) { return value == null ? CommandHistorySourceEnum.HTTP.getIndex() : value.getIndex(); }
    private LocalDateTime local(Instant value) { return LocalDateTime.ofInstant(value == null ? Instant.now() : value, ZoneOffset.UTC); }
    private LocalDateTime utcNow() { return LocalDateTime.now(ZoneOffset.UTC); }
    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset) return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return null;
    }
    private String text(String value) { return value; }
    private <T> DatabaseClient.GenericExecuteSpec bindNullable(DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }
}
