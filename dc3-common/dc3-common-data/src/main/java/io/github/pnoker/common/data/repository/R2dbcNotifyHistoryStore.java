package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.NotifyHistoryDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.utils.JsonUtil;
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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Explicit SQL adapter for {@code dc3_data.dc3_notify_history}. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcNotifyHistoryStore implements ReactiveNotifyHistoryStore {

    private static final String TABLE = "dc3_data.dc3_notify_history";
    private static final String COLUMNS = "id,rule_id,notify_id,message_id,channel_id,alarm_id,dedupe_key,channel_type_flag,target,"
            + "status_flag,request_ext,response_ext,error_message,retry_count,tenant_id,remark,creator_id,creator_name,"
            + "create_time,operator_id,operator_name,operate_time";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<NotifyHistoryDO> get(long tenantId, long historyId) {
        if (tenantId <= 0 || historyId <= 0) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND id=:id LIMIT 1")
                .bind("tenant_id", tenantId).bind("id", historyId).map(this::map).one();
    }

    @Override
    public Mono<OffsetPage<NotifyHistoryDO>> list(long tenantId, Long ruleId, Long notifyId, Long messageId,
                                                    Long channelId, Long alarmId, io.github.pnoker.common.enums.NotifyChannelTypeEnum channelTypeFlag,
                                                    String target, io.github.pnoker.common.enums.NotifyHistoryStatusEnum statusFlag,
                                                    PageRequest page) {
        if (tenantId <= 0) return Mono.just(OffsetPage.of(List.of(), page.offset(), page.limit(), 0));
        StringBuilder where = new StringBuilder(" WHERE tenant_id=:tenant_id");
        List<String> names = new ArrayList<>();
        if (ruleId != null) { where.append(" AND rule_id=:rule_id"); names.add("rule_id"); }
        if (notifyId != null) { where.append(" AND notify_id=:notify_id"); names.add("notify_id"); }
        if (messageId != null) { where.append(" AND message_id=:message_id"); names.add("message_id"); }
        if (channelId != null) { where.append(" AND channel_id=:channel_id"); names.add("channel_id"); }
        if (alarmId != null) { where.append(" AND alarm_id=:alarm_id"); names.add("alarm_id"); }
        if (channelTypeFlag != null) { where.append(" AND channel_type_flag=:channel_type"); names.add("channel_type"); }
        if (target != null && !target.isBlank()) { where.append(" AND target LIKE :target"); names.add("target"); }
        if (statusFlag != null) { where.append(" AND status_flag=:status"); names.add("status"); }
        String condition = where.toString();
        DatabaseClient.GenericExecuteSpec countSpec = databaseClient.sql("SELECT COUNT(*) FROM " + TABLE + condition).bind("tenant_id", tenantId);
        DatabaseClient.GenericExecuteSpec rowsSpec = databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + condition
                        + " ORDER BY " + orderBy(page.sort()) + " LIMIT :limit OFFSET :offset").bind("tenant_id", tenantId)
                .bind("limit", page.limit()).bind("offset", page.offset());
        for (String name : names) {
            Object value = switch (name) {
                case "rule_id" -> ruleId;
                case "notify_id" -> notifyId;
                case "message_id" -> messageId;
                case "channel_id" -> channelId;
                case "alarm_id" -> alarmId;
                case "channel_type" -> channelTypeFlag.getIndex();
                case "target" -> "%" + target + "%";
                case "status" -> statusFlag.getIndex();
                default -> null;
            };
            countSpec = countSpec.bind(name, value); rowsSpec = rowsSpec.bind(name, value);
        }
        Mono<Long> total = countSpec.map((row, metadata) -> row.get(0, Long.class)).one().defaultIfEmpty(0L);
        DatabaseClient.GenericExecuteSpec itemRows = rowsSpec;
        return total.flatMap(totalCount -> itemRows.map(this::map).all().collectList()
                        .map(items -> OffsetPage.of(items, page.offset(), page.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<Boolean> delete(long tenantId, long historyId) {
        if (tenantId <= 0 || historyId <= 0) return Mono.just(false);
        return databaseClient.sql("DELETE FROM " + TABLE + " WHERE tenant_id=:tenant_id AND id=:id")
                .bind("tenant_id", tenantId).bind("id", historyId).fetch().rowsUpdated().map(rows -> rows == 1);
    }

    @Override
    public Mono<NotifyHistoryDO> insert(NotifyHistoryDO history) {
        return insertIdempotent(history).map(NotifyHistoryInsertResult::history);
    }

    @Override
    public Mono<NotifyHistoryInsertResult> insertIdempotent(NotifyHistoryDO history) {
        if (history == null || history.getTenantId() == null || history.getTenantId() <= 0) {
            return Mono.error(new IllegalArgumentException("tenantId is required"));
        }
        if (history.getId() == null) history.setId(UuidV7.nextLong());
        LocalDateTime now = utcNow();
        if (history.getCreateTime() == null) history.setCreateTime(now);
        if (history.getOperateTime() == null) history.setOperateTime(now);
        if (history.getRetryCount() == null) history.setRetryCount(0);
        String baseSql = "INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES (:id,:rule_id,:notify_id,:message_id,:channel_id,:alarm_id,:dedupe_key,:channel_type_flag,:target,:status_flag,"
                + dialect.jsonWriteExpression(":request_ext") + "," + dialect.jsonWriteExpression(":response_ext") + ",:error_message,:retry_count,:tenant_id,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time)";
        boolean deduplicated = history.getDedupeKey() != null && !history.getDedupeKey().isBlank();
        String sql = deduplicated
                ? ("postgres".equals(dialect.name()) ? baseSql + " ON CONFLICT (tenant_id,dedupe_key) DO NOTHING"
                : baseSql + " ON DUPLICATE KEY UPDATE id=id")
                : baseSql;
        DatabaseClient.GenericExecuteSpec spec = insertSpec(history, sql);
        Mono<Long> affected = spec.fetch().rowsUpdated();
        if (!deduplicated) {
            return transactionalOperator.transactional(affected).flatMap(rows -> rows == 1
                    ? Mono.just(new NotifyHistoryInsertResult(history, true))
                    : Mono.error(new IllegalStateException("notify history insert affected " + rows + " rows")));
        }
        return transactionalOperator.transactional(affected
                        .flatMap(ignored -> findByDedupe(history.getTenantId(), history.getDedupeKey())
                                .switchIfEmpty(Mono.error(new IllegalStateException("notify history dedupe row disappeared")))
                                .map(existing -> new NotifyHistoryInsertResult(existing, history.getId().equals(existing.getId())))))
                .filter(result -> result.history() != null);
    }

    @Override
    public Mono<Boolean> updateDelivery(long tenantId, long historyId, byte statusFlag, String target,
                                        Object responseExt, String errorMessage, int retryCount) {
        if (tenantId <= 0 || historyId <= 0) return Mono.just(false);
        String sql = "UPDATE " + TABLE + " SET status_flag=:status_flag,target=:target,response_ext="
                + dialect.jsonWriteExpression(":response_ext") + ",error_message=:error_message,retry_count=:retry_count"
                + " WHERE tenant_id=:tenant_id AND id=:id";
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql).bind("status_flag", statusFlag)
                .bind("target", text(target)).bind("error_message", text(errorMessage)).bind("retry_count", retryCount)
                .bind("tenant_id", tenantId).bind("id", historyId);
        spec = spec.bind("response_ext", serializeJsonOrEmpty(responseExt));
        return transactionalOperator.transactional(spec.fetch().rowsUpdated()).map(rows -> rows == 1);
    }

    private NotifyHistoryDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        NotifyHistoryDO value = new NotifyHistoryDO();
        value.setId(row.get("id", Long.class)); value.setRuleId(row.get("rule_id", Long.class));
        value.setNotifyId(row.get("notify_id", Long.class)); value.setMessageId(row.get("message_id", Long.class));
        value.setChannelId(row.get("channel_id", Long.class)); value.setAlarmId(row.get("alarm_id", Long.class));
        value.setDedupeKey(row.get("dedupe_key", String.class));
        value.setChannelTypeFlag(number(row.get("channel_type_flag", Number.class))); value.setTarget(row.get("target", String.class));
        value.setStatusFlag(number(row.get("status_flag", Number.class))); value.setRequestExt(json(row.get("request_ext", String.class)));
        value.setResponseExt(json(row.get("response_ext", String.class))); value.setErrorMessage(row.get("error_message", String.class));
        value.setRetryCount(row.get("retry_count", Integer.class)); value.setTenantId(row.get("tenant_id", Long.class));
        value.setRemark(row.get("remark", String.class)); value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class)); value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class)); value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(time(row.get("operate_time"))); return value;
    }
    private JsonExt json(String raw) {
        if (raw == null) return null;
        try { return JsonUtil.parseObject(raw, JsonExt.class); }
        catch (RuntimeException ignored) { return null; }
    }
    private Byte number(Number value) { return value == null ? null : value.byteValue(); }
    private LocalDateTime time(Object raw) { if (raw instanceof LocalDateTime value) return value; if (raw instanceof OffsetDateTime value) return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime(); return raw instanceof java.time.Instant value ? LocalDateTime.ofInstant(value, ZoneOffset.UTC) : null; }
    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "create_time DESC, id DESC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column = switch (spec.field()) {
                case "ruleId" -> "rule_id"; case "notifyId" -> "notify_id"; case "messageId" -> "message_id";
                case "channelId" -> "channel_id"; case "alarmId" -> "alarm_id"; case "statusFlag" -> "status_flag";
                case "retryCount" -> "retry_count"; case "createTime" -> "create_time"; case "id" -> "id";
                default -> throw new IllegalArgumentException("unsupported sort field: " + spec.field());
            };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("id "))) clauses.add("id DESC");
        return String.join(", ", clauses);
    }

    private String serializeJson(Object value) { return value == null ? null : JsonUtil.toJsonString(value); }
    private String serializeJsonOrEmpty(Object value) { return value == null ? "{}" : serializeJson(value); }
    private String text(String value) { return value == null ? "" : value; }
    private Long value(Long value) { return value == null ? 0L : value; }
    private Byte value(Byte value) { return value == null ? (byte) 0 : value; }
    private Byte value(Enum<?> value) {
        if (value == null) return 0;
        if (value instanceof io.github.pnoker.common.enums.NotifyChannelTypeEnum channel) return channel.getIndex();
        if (value instanceof io.github.pnoker.common.enums.NotifyHistoryStatusEnum status) return status.getIndex();
        return 0;
    }
    private LocalDateTime utcNow() { return LocalDateTime.now(ZoneOffset.UTC); }
    private DatabaseClient.GenericExecuteSpec insertSpec(NotifyHistoryDO history, String sql) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql)
                .bind("id", history.getId()).bind("rule_id", value(history.getRuleId())).bind("notify_id", value(history.getNotifyId()))
                .bind("message_id", value(history.getMessageId())).bind("channel_id", value(history.getChannelId())).bind("alarm_id", value(history.getAlarmId()))
                .bind("channel_type_flag", value(history.getChannelTypeFlag())).bind("target", text(history.getTarget()))
                .bind("status_flag", value(history.getStatusFlag())).bind("error_message", text(history.getErrorMessage()))
                .bind("retry_count", history.getRetryCount()).bind("tenant_id", history.getTenantId()).bind("remark", text(history.getRemark()))
                .bind("creator_id", value(history.getCreatorId())).bind("creator_name", text(history.getCreatorName()))
                .bind("operator_id", value(history.getOperatorId())).bind("operator_name", text(history.getOperatorName()));
        spec = bindNullable(spec, "dedupe_key", history.getDedupeKey(), String.class);
        spec = bindTime(spec, "create_time", history.getCreateTime());
        spec = bindTime(spec, "operate_time", history.getOperateTime());
        spec = spec.bind("request_ext", serializeJsonOrEmpty(history.getRequestExt()));
        return spec.bind("response_ext", serializeJsonOrEmpty(history.getResponseExt()));
    }
    private Mono<NotifyHistoryDO> findByDedupe(long tenantId, String dedupeKey) {
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND dedupe_key=:dedupe_key LIMIT 1")
                .bind("tenant_id", tenantId).bind("dedupe_key", dedupeKey).map(this::map).one();
    }
    private DatabaseClient.GenericExecuteSpec bindTime(DatabaseClient.GenericExecuteSpec spec, String name, LocalDateTime value) {
        return "postgres".equals(dialect.name())
                ? spec.bind(name, OffsetDateTime.of(value, ZoneOffset.UTC))
                : spec.bind(name, value);
    }
    private <T> DatabaseClient.GenericExecuteSpec bindNullable(DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }
}
