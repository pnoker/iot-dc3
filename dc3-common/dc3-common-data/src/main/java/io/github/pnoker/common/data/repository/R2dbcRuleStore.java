package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.data.entity.model.RuleDO;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Repository
@ConditionalOnClass({DatabaseClient.class, R2dbcDialect.class, TransactionalOperator.class, PageTransaction.class})
@RequiredArgsConstructor
public class R2dbcRuleStore implements ReactiveRuleStore {
    private static final String TABLE = "dc3_data.dc3_rule";
    private static final String COLUMNS = "id,alarm_target_type_flag,rule_name,rule_code,entity_id,notify_id,message_id,rule_ext,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted";
    private final DatabaseClient databaseClient;
    private final RuleBuilder ruleBuilder;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Flux<RuleBO> listEnabledCandidates(long tenantId, AlarmTargetTypeEnum targetType, long entityId) {
        if (tenantId <= 0 || targetType == null || entityId <= 0) return Flux.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND alarm_target_type_flag=:target_type AND enable_flag=0 AND deleted=0"
                        + " AND (entity_id=:entity_id OR entity_id=0) ORDER BY id")
                .bind("tenant_id", tenantId).bind("target_type", targetType.getIndex()).bind("entity_id", entityId)
                .map((row, metadata) -> ruleBuilder.buildBOByDO(map(row))).all();
    }

    @Override
    public Mono<RuleDO> get(long tenantId, long id) {
        if (!valid(tenantId) || !valid(id)) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId).bind("id", id).map((row, metadata) -> map(row)).one();
    }

    @Override
    public Mono<OffsetPage<RuleDO>> list(long tenantId, String ruleName, String ruleCode, Long entityId,
                                         AlarmTargetTypeEnum targetType, EnableFlagEnum enableFlag,
                                         PageRequest pageRequest) {
        if (!valid(tenantId)) return Mono.just(OffsetPage.of(List.of(), pageRequest.offset(), pageRequest.limit(), 0));
        StringBuilder predicates = new StringBuilder(" WHERE tenant_id=:tenant_id AND deleted=0");
        if (ruleName != null && !ruleName.isBlank()) predicates.append(" AND rule_name LIKE :rule_name");
        if (ruleCode != null && !ruleCode.isBlank()) predicates.append(" AND rule_code=:rule_code");
        if (entityId != null) predicates.append(" AND entity_id=:entity_id");
        if (targetType != null) predicates.append(" AND alarm_target_type_flag=:target_type");
        if (enableFlag != null) predicates.append(" AND enable_flag=:enable_flag");
        DatabaseClient.GenericExecuteSpec count = databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + predicates)
                .bind("tenant_id", tenantId);
        DatabaseClient.GenericExecuteSpec rows = databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + predicates
                        + " ORDER BY " + orderBy(pageRequest.sort()) + " LIMIT :limit OFFSET :offset")
                .bind("tenant_id", tenantId).bind("limit", pageRequest.limit()).bind("offset", pageRequest.offset());
        count = bindFilters(count, ruleName, ruleCode, entityId, targetType, enableFlag);
        rows = bindFilters(rows, ruleName, ruleCode, entityId, targetType, enableFlag);
        Mono<Long> total = count.map((row, metadata) -> {
            Number value = row.get("total", Number.class);
            return value == null ? 0L : value.longValue();
        }).one().defaultIfEmpty(0L);
        DatabaseClient.GenericExecuteSpec itemRows = rows;
        return total.flatMap(totalCount -> itemRows.map((row, metadata) -> map(row)).all().collectList()
                        .map(items -> OffsetPage.of(items, pageRequest.offset(), pageRequest.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<RuleDO> insert(RuleDO rule) {
        if (rule == null || !valid(rule.getTenantId())) return Mono.error(new IllegalArgumentException("tenantId is required"));
        if (rule.getId() == null) rule.setId(UuidV7.nextLong());
        LocalDateTime now = utcNow();
        if (rule.getCreateTime() == null) rule.setCreateTime(now);
        if (rule.getOperateTime() == null) rule.setOperateTime(now);
        String sql = "INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES (:id,:alarm_target_type_flag,:rule_name,:rule_code,:entity_id,:notify_id,:message_id,"
                + dialect.jsonWriteExpression(":rule_ext") + ",:enable_flag,:tenant_id,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql)
                .bind("id", rule.getId()).bind("alarm_target_type_flag", value(rule.getAlarmTargetTypeFlag()))
                .bind("rule_name", text(rule.getRuleName())).bind("rule_code", text(rule.getRuleCode()))
                .bind("entity_id", value(rule.getEntityId())).bind("notify_id", value(rule.getNotifyId()))
                .bind("message_id", value(rule.getMessageId())).bind("enable_flag", value(rule.getEnableFlag()))
                .bind("tenant_id", rule.getTenantId()).bind("remark", text(rule.getRemark()))
                .bind("creator_id", value(rule.getCreatorId())).bind("creator_name", text(rule.getCreatorName()))
                .bind("create_time", rule.getCreateTime()).bind("operator_id", value(rule.getOperatorId()))
                .bind("operator_name", text(rule.getOperatorName())).bind("operate_time", rule.getOperateTime());
        spec = bindNullable(spec, "rule_ext", rule.getRuleExt() == null ? null : JsonUtil.toJsonString(rule.getRuleExt()), String.class);
        return transactionalOperator.transactional(spec.fetch().rowsUpdated())
                .flatMap(rows -> rows == 1 ? get(rule.getTenantId(), rule.getId())
                        : Mono.error(new IllegalStateException("rule insert affected " + rows + " rows")));
    }

    @Override
    public Mono<RuleDO> update(RuleDO rule) {
        if (rule == null || !valid(rule.getTenantId()) || !valid(rule.getId())) return Mono.empty();
        String sql = "UPDATE " + TABLE + " SET alarm_target_type_flag=:alarm_target_type_flag,rule_name=:rule_name,rule_code=:rule_code,entity_id=:entity_id,notify_id=:notify_id,message_id=:message_id,rule_ext="
                + dialect.jsonWriteExpression(":rule_ext") + ",enable_flag=:enable_flag,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0";
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql)
                .bind("alarm_target_type_flag", value(rule.getAlarmTargetTypeFlag())).bind("rule_name", text(rule.getRuleName()))
                .bind("rule_code", text(rule.getRuleCode())).bind("entity_id", value(rule.getEntityId()))
                .bind("notify_id", value(rule.getNotifyId())).bind("message_id", value(rule.getMessageId()))
                .bind("enable_flag", value(rule.getEnableFlag())).bind("remark", text(rule.getRemark()))
                .bind("operator_id", value(rule.getOperatorId())).bind("operator_name", text(rule.getOperatorName()))
                .bind("operate_time", utcNow()).bind("tenant_id", rule.getTenantId()).bind("id", rule.getId());
        spec = bindNullable(spec, "rule_ext", rule.getRuleExt() == null ? null : JsonUtil.toJsonString(rule.getRuleExt()), String.class);
        return transactionalOperator.transactional(spec.fetch().rowsUpdated())
                .flatMap(rows -> rows == 1 ? get(rule.getTenantId(), rule.getId()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> softDelete(long tenantId, long id) {
        if (!valid(tenantId) || !valid(id)) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient.sql("UPDATE " + TABLE
                        + " SET deleted=1,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0")
                .bind("operate_time", utcNow()).bind("tenant_id", tenantId).bind("id", id).fetch().rowsUpdated())
                .map(rows -> rows == 1);
    }

    @Override
    public Mono<Boolean> hasChildren(long tenantId, long id) {
        if (!valid(tenantId) || !valid(id)) return Mono.just(false);
        return databaseClient.sql("SELECT 1 FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND entity_id=:entity_id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId).bind("entity_id", id).map((row, metadata) -> true).one().defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> existsActiveCode(long tenantId, String ruleCode, Long excludedId) {
        if (!valid(tenantId) || ruleCode == null || ruleCode.isBlank()) return Mono.just(false);
        String sql = "SELECT 1 FROM " + TABLE + " WHERE tenant_id=:tenant_id AND rule_code=:rule_code AND deleted=0"
                + (excludedId == null ? "" : " AND id<>:excluded_id") + " LIMIT 1";
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql).bind("tenant_id", tenantId).bind("rule_code", ruleCode);
        if (excludedId != null) spec = spec.bind("excluded_id", excludedId);
        return spec.map((row, metadata) -> true).one().defaultIfEmpty(false);
    }

    private RuleDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        return map(row);
    }

    private RuleDO map(io.r2dbc.spi.Row row) {
        RuleDO value = new RuleDO();
        value.setId(number(row.get("id")));
        value.setAlarmTargetTypeFlag(byteValue(row.get("alarm_target_type_flag")));
        value.setRuleName(text(row.get("rule_name")));
        value.setRuleCode(text(row.get("rule_code")));
        value.setEntityId(number(row.get("entity_id")));
        value.setNotifyId(number(row.get("notify_id")));
        value.setMessageId(number(row.get("message_id")));
        String ruleExt = row.get("rule_ext", String.class);
        value.setRuleExt(ruleExt == null ? null : JsonUtil.parseObject(ruleExt, io.github.pnoker.common.entity.ext.JsonExt.class));
        value.setEnableFlag(byteValue(row.get("enable_flag")));
        value.setTenantId(number(row.get("tenant_id")));
        value.setRemark(text(row.get("remark")));
        value.setCreatorId(number(row.get("creator_id")));
        value.setCreatorName(text(row.get("creator_name")));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(number(row.get("operator_id")));
        value.setOperatorName(text(row.get("operator_name")));
        value.setOperateTime(time(row.get("operate_time")));
        value.setDeleted(byteValue(row.get("deleted")));
        return value;
    }
    private long number(Object value){return value instanceof Number n?n.longValue():0L;}
    private Byte byteValue(Object value){return value instanceof Number n?n.byteValue():null;}
    private String text(Object value){return value==null?null:value.toString();}
    private LocalDateTime time(Object value){if(value instanceof LocalDateTime l)return l;if(value instanceof Instant i)return LocalDateTime.ofInstant(i, ZoneOffset.UTC);if(value instanceof OffsetDateTime o)return o.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();return null;}
    private DatabaseClient.GenericExecuteSpec bindFilters(DatabaseClient.GenericExecuteSpec spec, String ruleName, String ruleCode,
                                                          Long entityId, AlarmTargetTypeEnum targetType, EnableFlagEnum enableFlag) {
        if (ruleName != null && !ruleName.isBlank()) spec = spec.bind("rule_name", "%" + ruleName.trim() + "%");
        if (ruleCode != null && !ruleCode.isBlank()) spec = spec.bind("rule_code", ruleCode.trim());
        if (entityId != null) spec = spec.bind("entity_id", entityId);
        if (targetType != null) spec = spec.bind("target_type", targetType.getIndex());
        if (enableFlag != null) spec = spec.bind("enable_flag", enableFlag.getIndex());
        return spec;
    }
    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "create_time DESC,id DESC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column = switch (spec.field()) {
                case "id" -> "id"; case "ruleName" -> "rule_name"; case "ruleCode" -> "rule_code";
                case "entityId" -> "entity_id"; case "enableFlag" -> "enable_flag";
                case "createTime" -> "create_time"; case "operateTime" -> "operate_time";
                default -> throw new IllegalArgumentException("rule sort field is not allowed: " + spec.field());
            };
            clauses.add(column + (spec.direction() == SortSpec.Direction.ASC ? " ASC" : " DESC"));
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("id "))) clauses.add("id DESC");
        return String.join(",", clauses);
    }
    private String text(String value) { return value == null ? "" : value; }
    private long value(Long value) { return value == null ? 0L : value; }
    private byte value(Byte value) { return value == null ? (byte) 0 : value; }
    private byte value(AlarmTargetTypeEnum value) { return value == null ? 0 : value.getIndex(); }
    private byte value(EnableFlagEnum value) { return value == null ? 0 : value.getIndex(); }
    private LocalDateTime utcNow() { return LocalDateTime.now(ZoneOffset.UTC); }
    private boolean valid(Long value) { return value != null && value > 0; }
    private boolean valid(long value) { return value > 0; }
    private <T> DatabaseClient.GenericExecuteSpec bindNullable(DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }
}
