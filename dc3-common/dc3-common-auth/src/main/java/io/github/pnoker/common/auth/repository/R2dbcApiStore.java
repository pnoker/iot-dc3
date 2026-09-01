package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.ApiTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
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
import java.util.Optional;

@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcApiStore implements ReactiveApiStore {

    private static final String TABLE = "dc3_auth.dc3_api";
    private static final String COLUMNS = "id,service_name,api_type_flag,api_name,api_code,api_group,api_ext,"
            + "enable_flag,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted";
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override public Mono<ApiDO> getById(Long id) {
        if (!valid(id)) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE id=:id AND deleted=0 LIMIT 1")
                .bind("id", id).map(this::map).one();
    }

    @Override public Mono<OffsetPage<ApiDO>> list(ApiFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("api filter is required"));
        String where = where(filter);
        DatabaseClient.GenericExecuteSpec count = bind(databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + where), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + where
                                + " ORDER BY " + orderBy(filter.page().sort()) + " LIMIT :limit OFFSET :offset"), filter)
                .bind("limit", filter.page().limit()).bind("offset", filter.page().offset());
        Mono<Long> total = count.map((row, metadata) -> Optional.ofNullable(row.get("total", Number.class))
                        .map(Number::longValue).orElse(0L)).one().defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(this::map).all().collectList()
                        .map(items -> OffsetPage.of(items, filter.page().offset(), filter.page().limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override public Mono<ApiDO> insert(ApiBO api) {
        if (api == null) return Mono.error(new IllegalArgumentException("api is required"));
        long id = UuidV7.nextLong(); LocalDateTime now = now();
        String sql = "INSERT INTO " + TABLE + " (id,service_name,api_type_flag,api_name,api_code,api_group,api_ext,"
                + "enable_flag,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                + " VALUES (:id,:service_name,:api_type_flag,:api_name,:api_code,:api_group,"
                + dialect.jsonWriteExpression(":api_ext") + ",:enable_flag,:remark,:creator_id,:creator_name,:create_time,"
                + ":operator_id,:operator_name,:operate_time,0)";
        DatabaseClient.GenericExecuteSpec query = databaseClient.sql(sql).bind("id", id)
                .bind("service_name", text(api.getServiceName())).bind("api_type_flag", index(api.getApiTypeFlag()))
                .bind("api_name", text(api.getApiName())).bind("api_code", text(api.getApiCode()))
                .bind("api_group", text(api.getApiGroup())).bind("api_ext", json(api.getApiExt()))
                .bind("enable_flag", index(api.getEnableFlag())).bind("remark", text(api.getRemark()))
                .bind("creator_id", value(api.getCreatorId())).bind("creator_name", text(api.getCreatorName()))
                .bind("create_time", now).bind("operator_id", value(api.getOperatorId()))
                .bind("operator_name", text(api.getOperatorName())).bind("operate_time", now);
        return transactionalOperator.transactional(query.fetch().rowsUpdated()).then(getById(id));
    }

    @Override public Mono<ApiDO> update(ApiBO api) {
        if (api == null || !valid(api.getId())) return Mono.empty();
        String sql = "UPDATE " + TABLE + " SET service_name=:service_name,api_type_flag=:api_type_flag,api_name=:api_name,"
                + "api_code=:api_code,api_group=:api_group,api_ext=" + dialect.jsonWriteExpression(":api_ext")
                + ",enable_flag=:enable_flag,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                + " WHERE id=:id AND deleted=0";
        DatabaseClient.GenericExecuteSpec query = databaseClient.sql(sql).bind("id", api.getId())
                .bind("service_name", text(api.getServiceName())).bind("api_type_flag", index(api.getApiTypeFlag()))
                .bind("api_name", text(api.getApiName())).bind("api_code", text(api.getApiCode()))
                .bind("api_group", text(api.getApiGroup())).bind("api_ext", json(api.getApiExt()))
                .bind("enable_flag", index(api.getEnableFlag())).bind("remark", text(api.getRemark()))
                .bind("operator_id", value(api.getOperatorId())).bind("operator_name", text(api.getOperatorName()))
                .bind("operate_time", now());
        return transactionalOperator.transactional(query.fetch().rowsUpdated()).then(getById(api.getId()));
    }

    @Override public Mono<Boolean> delete(Long id, Long operatorId, String operatorName) {
        if (!valid(id)) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient.sql("UPDATE " + TABLE
                                + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                                + " WHERE id=:id AND deleted=0").bind("id", id)
                        .bind("operator_id", value(operatorId)).bind("operator_name", text(operatorName))
                        .bind("operate_time", now()).fetch().rowsUpdated()).map(updated -> updated == 1);
    }

    @Override public Mono<Boolean> existsDuplicate(ApiBO api) {
        if (api == null || api.getApiCode() == null || api.getApiCode().isBlank()) return Mono.just(false);
        String predicate = " WHERE api_code=:api_code AND deleted=0";
        if (api.getId() != null) predicate += " AND id<>:id";
        DatabaseClient.GenericExecuteSpec query = databaseClient.sql("SELECT 1 FROM " + TABLE + predicate + " LIMIT 1")
                .bind("api_code", api.getApiCode().trim());
        if (api.getId() != null) query = query.bind("id", api.getId());
        return query.fetch().first().hasElement();
    }

    private String where(ApiFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE deleted=0");
        if (filter.serviceName() != null) where.append(" AND service_name=:service_name");
        if (filter.apiTypeFlag() != null) where.append(" AND api_type_flag=:api_type_flag");
        if (filter.apiName() != null) where.append(" AND ").append(dialect.caseInsensitiveLike("api_name", ":api_name"));
        if (filter.apiCode() != null) where.append(" AND api_code=:api_code");
        if (filter.apiGroup() != null) where.append(" AND api_group=:api_group");
        if (filter.enableFlag() != null) where.append(" AND enable_flag=:enable_flag");
        return where.toString();
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, ApiFilter filter) {
        if (filter.serviceName() != null) spec = spec.bind("service_name", filter.serviceName());
        if (filter.apiTypeFlag() != null) spec = spec.bind("api_type_flag", filter.apiTypeFlag().getIndex());
        if (filter.apiName() != null) spec = spec.bind("api_name", "%" + filter.apiName() + "%");
        if (filter.apiCode() != null) spec = spec.bind("api_code", filter.apiCode());
        if (filter.apiGroup() != null) spec = spec.bind("api_group", filter.apiGroup());
        if (filter.enableFlag() != null) spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        List<String> clauses = new ArrayList<>();
        if (sort != null) for (SortSpec spec : sort) {
            String column = switch (spec.field()) {
                case "id" -> "id"; case "serviceName" -> "service_name"; case "apiName" -> "api_name";
                case "apiCode" -> "api_code"; case "apiGroup" -> "api_group";
                case "apiTypeFlag" -> "api_type_flag"; case "enableFlag" -> "enable_flag";
                default -> throw new IllegalArgumentException("unsupported API sort field: " + spec.field());
            };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("id "))) clauses.add("id ASC");
        return String.join(",", clauses);
    }

    private ApiDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        ApiDO api = new ApiDO(); api.setId(row.get("id", Long.class)); api.setServiceName(row.get("service_name", String.class));
        api.setApiTypeFlag(number(row.get("api_type_flag"))); api.setApiName(row.get("api_name", String.class));
        api.setApiCode(row.get("api_code", String.class)); api.setApiGroup(row.get("api_group", String.class));
        api.setApiExt(parseJson(row.get("api_ext", String.class))); api.setEnableFlag(number(row.get("enable_flag")));
        api.setRemark(row.get("remark", String.class)); api.setCreatorId(row.get("creator_id", Long.class));
        api.setCreatorName(row.get("creator_name", String.class)); api.setCreateTime(time(row.get("create_time")));
        api.setOperatorId(row.get("operator_id", Long.class)); api.setOperatorName(row.get("operator_name", String.class));
        api.setOperateTime(time(row.get("operate_time"))); api.setDeleted(number(row.get("deleted"))); return api;
    }

    private JsonExt parseJson(String value) { return value == null ? null : JsonUtil.parseObject(value, JsonExt.class); }
    private Byte number(Object value) { return value instanceof Number number ? number.byteValue() : null; }
    private LocalDateTime time(Object value) { if (value instanceof LocalDateTime local) return local; if (value instanceof OffsetDateTime offset) return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime(); if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC); return null; }
    private String json(Object value) { return value == null ? "{}" : JsonUtil.toJsonString(value); }
    private String text(String value) { return value == null ? "" : value.trim(); }
    private long value(Long value) { return value == null ? 0L : value; }
    private byte index(Object value) { if (value instanceof EnableFlagEnum flag) return flag.getIndex(); if (value instanceof ApiTypeEnum flag) return flag.getIndex(); return 0; }
    private LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private boolean valid(Long id) { return id != null && id > 0; }
}
