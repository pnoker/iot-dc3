package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Explicit SQL adapter for per-device command attribute configuration. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcCommandAttributeConfigStore implements ReactiveCommandAttributeConfigStore {
    private static final String TABLE = "dc3_manager.dc3_command_attribute_config";
    private static final String COLUMNS = "id, attribute_id, config_value, device_id, config_ext, command_id, enable_flag, tenant_id, remark, signature, version, creator_id, creator_name, create_time, operator_id, operator_name, operate_time";
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final ObjectMapper objectMapper;
    private final R2dbcDialect dialect;

    @Override public Mono<CommandAttributeConfigBO> get(Long tenantId, Long id) {
        if (tenantId == null || id == null || tenantId <= 0 || id <= 0) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1").bind("tenant_id", tenantId).bind("id", id).map(this::map).one();
    }
    @Override public Mono<CommandAttributeConfigBO> getByAttributeDeviceCommand(Long tenantId, Long attributeId, Long deviceId, Long commandId) {
        if (tenantId == null || attributeId == null || deviceId == null || commandId == null) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND attribute_id=:attribute_id AND device_id=:device_id AND command_id=:command_id AND deleted=0 LIMIT 1").bind("tenant_id", tenantId).bind("attribute_id", attributeId).bind("device_id", deviceId).bind("command_id", commandId).map(this::map).one();
    }
    @Override public Flux<CommandAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null || tenantId <= 0 || deviceId <= 0) return Flux.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND device_id=:device_id AND deleted=0 ORDER BY id ASC").bind("tenant_id", tenantId).bind("device_id", deviceId).map(this::map).all();
    }
    @Override public Flux<CommandAttributeConfigBO> listByDeviceIdAndCommandId(Long tenantId, Long deviceId, Long commandId) {
        if (tenantId == null || deviceId == null || commandId == null || tenantId <= 0 || deviceId <= 0 || commandId <= 0) return Flux.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND device_id=:device_id AND command_id=:command_id AND deleted=0 ORDER BY id ASC").bind("tenant_id", tenantId).bind("device_id", deviceId).bind("command_id", commandId).map(this::map).all();
    }
    @Override public Mono<CommandAttributeConfigBO> insert(CommandAttributeConfigBO value) {
        if (value == null || value.getTenantId() == null || value.getAttributeId() == null || value.getDeviceId() == null || value.getCommandId() == null) return Mono.error(new IllegalArgumentException("tenantId, attributeId, deviceId and commandId are required"));
        if (value.getId() == null) value.setId(UuidV7.nextLong()); if (value.getVersion() == null) value.setVersion(0); LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DatabaseClient.GenericExecuteSpec query = databaseClient.sql("INSERT INTO " + TABLE + " (" + COLUMNS + ", deleted) VALUES (:id,:attribute_id,:config_value,:device_id," + dialect.jsonWriteExpression(":config_ext") + ",:command_id,:enable_flag,:tenant_id,:remark,:signature,:version,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)").bind("id", value.getId()).bind("attribute_id", value.getAttributeId()).bind("config_value", value.getConfigValue() == null ? "" : value.getConfigValue()).bind("device_id", value.getDeviceId()).bind("config_ext", serialize(value.getConfigExt())).bind("command_id", value.getCommandId()).bind("enable_flag", index(value.getEnableFlag())).bind("tenant_id", value.getTenantId()).bind("remark", value.getRemark() == null ? "" : value.getRemark()).bind("signature", value.getSignature() == null ? "" : value.getSignature()).bind("version", value.getVersion()).bind("creator_id", value.getCreatorId() == null ? 0L : value.getCreatorId()).bind("creator_name", value.getCreatorName() == null ? "" : value.getCreatorName()).bind("create_time", value.getCreateTime() == null ? now : value.getCreateTime()).bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId()).bind("operator_name", value.getOperatorName() == null ? "" : value.getOperatorName()).bind("operate_time", value.getOperateTime() == null ? now : value.getOperateTime());
        return transactionalOperator.transactional(query.fetch().rowsUpdated().flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.error(new IllegalStateException("command attribute config insert affected " + rows + " rows"))));
    }
    @Override public Mono<CommandAttributeConfigBO> update(CommandAttributeConfigBO value, int expectedVersion) {
        if (value == null || value.getTenantId() == null || value.getId() == null) return Mono.error(new IllegalArgumentException("tenantId and config id are required"));
        DatabaseClient.GenericExecuteSpec query = databaseClient.sql("UPDATE " + TABLE + " SET attribute_id=:attribute_id, config_value=:config_value, device_id=:device_id, config_ext=" + dialect.jsonWriteExpression(":config_ext") + ", command_id=:command_id, enable_flag=:enable_flag, remark=:remark, signature=:signature, version=version+1, operator_id=:operator_id, operator_name=:operator_name, operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND version=:expected_version AND deleted=0").bind("attribute_id", value.getAttributeId()).bind("config_value", value.getConfigValue() == null ? "" : value.getConfigValue()).bind("device_id", value.getDeviceId()).bind("config_ext", serialize(value.getConfigExt())).bind("command_id", value.getCommandId()).bind("enable_flag", index(value.getEnableFlag())).bind("remark", value.getRemark() == null ? "" : value.getRemark()).bind("signature", value.getSignature() == null ? "" : value.getSignature()).bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId()).bind("operator_name", value.getOperatorName() == null ? "" : value.getOperatorName()).bind("operate_time", LocalDateTime.now(ZoneOffset.UTC)).bind("tenant_id", value.getTenantId()).bind("id", value.getId()).bind("expected_version", expectedVersion);
        return transactionalOperator.transactional(query.fetch().rowsUpdated().flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }
    @Override public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient.sql("UPDATE " + TABLE + " SET deleted=1, operator_id=:operator_id, operator_name=:operator_name, operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND version=:expected_version AND deleted=0").bind("tenant_id", tenantId).bind("id", id).bind("expected_version", expectedVersion).bind("operator_id", operatorId == null ? 0L : operatorId).bind("operator_name", operatorName == null ? "" : operatorName).bind("operate_time", LocalDateTime.now(ZoneOffset.UTC)).fetch().rowsUpdated().map(rows -> rows == 1));
    }
    @Override public Mono<OffsetPage<CommandAttributeConfigBO>> list(CommandAttributeConfigFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("filter is required"));
        StringBuilder where = new StringBuilder(" WHERE c.tenant_id=:tenant_id AND c.deleted=0"); if (filter.attributeId() != null) where.append(" AND c.attribute_id=:attribute_id"); if (filter.deviceId() != null) where.append(" AND c.device_id=:device_id"); if (filter.commandId() != null) where.append(" AND c.command_id=:command_id"); if (filter.enableFlag() != null) where.append(" AND c.enable_flag=:enable_flag"); if (filter.version() != null) where.append(" AND c.version=:version"); String predicate = where.toString();
        DatabaseClient.GenericExecuteSpec count = bind(databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + " c" + predicate), filter); DatabaseClient.GenericExecuteSpec rows = bind(databaseClient.sql("SELECT " + qualifiedColumns("c") + " FROM " + TABLE + " c" + predicate + orderBy(filter.sort()) + " LIMIT :limit OFFSET :offset"), filter).bind("limit", filter.limit()).bind("offset", filter.offset()); Mono<Long> total = count.map((row, metadata) -> { Number n = row.get("total", Number.class); return n == null ? 0L : n.longValue(); }).one().defaultIfEmpty(0L); return total.flatMap(totalCount -> rows.map(this::map).all().collectList().map(items -> OffsetPage.of(items, filter.offset(), filter.limit(), totalCount))).as(pageTransaction::transactional);
    }
    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, CommandAttributeConfigFilter filter) { spec = spec.bind("tenant_id", filter.tenantId()); if (filter.attributeId() != null) spec = spec.bind("attribute_id", filter.attributeId()); if (filter.deviceId() != null) spec = spec.bind("device_id", filter.deviceId()); if (filter.commandId() != null) spec = spec.bind("command_id", filter.commandId()); if (filter.enableFlag() != null) spec = spec.bind("enable_flag", filter.enableFlag().getIndex()); if (filter.version() != null) spec = spec.bind("version", filter.version()); return spec; }
    private String orderBy(List<SortSpec> sort) { List<String> clauses = new ArrayList<>(); if (sort != null) for (SortSpec spec : sort) { String column = switch (spec.field()) { case "id" -> "c.id"; case "attributeId" -> "c.attribute_id"; case "deviceId" -> "c.device_id"; case "commandId" -> "c.command_id"; case "createTime" -> "c.create_time"; case "operateTime" -> "c.operate_time"; case "version" -> "c.version"; default -> throw new IllegalArgumentException("unsupported command attribute config sort field"); }; clauses.add(column + (spec.direction() == SortSpec.Direction.DESC ? " DESC" : " ASC")); } if (clauses.stream().noneMatch(v -> v.startsWith("c.id"))) clauses.add("c.id ASC"); return " ORDER BY " + String.join(", ", clauses); }
    private String qualifiedColumns(String alias) { return java.util.Arrays.stream(COLUMNS.split(", ")).map(c -> alias + "." + c).reduce((a, b) -> a + ", " + b).orElse(COLUMNS); }
    private CommandAttributeConfigBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) { CommandAttributeConfigBO value = new CommandAttributeConfigBO(); value.setId(row.get("id", Long.class)); value.setAttributeId(row.get("attribute_id", Long.class)); value.setConfigValue(row.get("config_value", String.class)); value.setDeviceId(row.get("device_id", Long.class)); value.setCommandId(row.get("command_id", Long.class)); value.setTenantId(row.get("tenant_id", Long.class)); value.setRemark(row.get("remark", String.class)); value.setSignature(row.get("signature", String.class)); value.setVersion(row.get("version", Integer.class)); value.setCreatorId(row.get("creator_id", Long.class)); value.setCreatorName(row.get("creator_name", String.class)); value.setCreateTime(time(row.get("create_time"))); value.setOperatorId(row.get("operator_id", Long.class)); value.setOperatorName(row.get("operator_name", String.class)); value.setOperateTime(time(row.get("operate_time"))); Number enabled = row.get("enable_flag", Number.class); value.setEnableFlag(EnableFlagEnum.ofIndex(enabled == null ? null : enabled.byteValue())); String raw = row.get("config_ext", String.class); if (raw != null) try { value.setConfigExt(objectMapper.readValue(raw, JsonExt.class)); } catch (Exception exception) { throw new IllegalStateException("config_ext contains invalid JSON", exception); } return value; }
    private LocalDateTime time(Object value) { if (value instanceof LocalDateTime local) return local; if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC); if (value instanceof OffsetDateTime offset) return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime(); return null; }
    private Byte index(EnableFlagEnum value) { return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex(); }
    private String serialize(JsonExt value) { try { return objectMapper.writeValueAsString(value == null ? new JsonExt() : value); } catch (Exception exception) { throw new IllegalArgumentException("config_ext is not valid JSON", exception); } }
}
