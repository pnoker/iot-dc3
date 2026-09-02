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

import io.github.pnoker.common.data.entity.bo.dashboard.AlertItemRow;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
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
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for dashboard alert reads and confirmation updates. */
@Repository
@ConditionalOnClass({DatabaseClient.class, PageTransaction.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcAlertStore implements ReactiveAlertStore {

    private static final String TABLE = "dc3_data.dc3_entity_alarm";
    private static final String SOURCE_EXPRESSION = "CASE a.alarm_target_type_flag"
            + " WHEN 0 THEN 'point' WHEN 1 THEN 'device' WHEN 2 THEN 'driver' ELSE 'event' END";

    private final DatabaseClient databaseClient;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<OffsetPage<AlertItemRow>> list(
            Long tenantId,
            String source,
            Integer alarmTypeFlag,
            Integer confirmFlag,
            LocalDateTime from,
            PageRequest page) {
        if (!valid(tenantId)) {
            return Mono.just(OffsetPage.of(List.of(), page.offset(), page.limit(), 0));
        }
        StringBuilder where = new StringBuilder(" WHERE a.tenant_id=:tenant_id");
        List<String> bindNames = new ArrayList<>();
        if (source != null && !source.isBlank()) {
            where.append(" AND a.alarm_target_type_flag=:target_type");
            bindNames.add("target_type");
        }
        if (alarmTypeFlag != null) {
            where.append(" AND a.alarm_type_flag=:alarm_type");
            bindNames.add("alarm_type");
        }
        if (confirmFlag != null) {
            where.append(" AND a.confirm_flag=:confirm_flag");
            bindNames.add("confirm_flag");
        }
        if (from != null) {
            where.append(" AND a.create_time>=:from_time");
            bindNames.add("from_time");
        }
        String predicate = where.toString();
        DatabaseClient.GenericExecuteSpec countSpec = databaseClient
                .sql("SELECT COUNT(*) AS total FROM " + TABLE + " a" + predicate)
                .bind("tenant_id", tenantId);
        DatabaseClient.GenericExecuteSpec rowsSpec = databaseClient
                .sql("SELECT a.id, " + SOURCE_EXPRESSION + " AS source, a.entity_id AS source_id,"
                        + " a.point_id, a.alarm_type_flag, a.confirm_flag, a.create_time, "
                        + messageExpression() + " AS message FROM " + TABLE + " a" + predicate
                        + " ORDER BY " + orderBy(page.sort()) + " LIMIT :limit OFFSET :offset")
                .bind("tenant_id", tenantId)
                .bind("limit", page.limit())
                .bind("offset", page.offset());
        for (String name : bindNames) {
            Object value =
                    switch (name) {
                        case "target_type" -> sourceIndex(source);
                        case "alarm_type" -> alarmTypeFlag;
                        case "confirm_flag" -> confirmFlag;
                        case "from_time" -> from;
                        default -> throw new IllegalStateException("unknown alert bind: " + name);
                    };
            countSpec = countSpec.bind(name, value);
            rowsSpec = rowsSpec.bind(name, value);
        }
        Mono<Long> total =
                countSpec.map((row, metadata) -> number(row.get("total"))).one().defaultIfEmpty(0L);
        DatabaseClient.GenericExecuteSpec itemRows = rowsSpec;
        return total.flatMap(totalCount -> itemRows.map(this::map)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(items, page.offset(), page.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<Boolean> updateConfirm(Long tenantId, String source, Long id, byte confirmFlag) {
        if (!valid(tenantId) || !valid(id) || confirmFlag < 0 || confirmFlag > 1 || source == null) {
            return Mono.just(false);
        }
        String sql = "UPDATE " + TABLE + " SET confirm_flag=:confirm_flag, operate_time=CURRENT_TIMESTAMP"
                + " WHERE tenant_id=:tenant_id AND id=:id AND alarm_target_type_flag=:target_type"
                + " AND confirm_flag<>:confirm_flag";
        return databaseClient
                .sql(sql)
                .bind("confirm_flag", confirmFlag)
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .bind("target_type", sourceIndex(source))
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1);
    }

    private AlertItemRow map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        AlertItemRow value = new AlertItemRow();
        value.setId(number(row.get("id")));
        value.setSource(text(row.get("source")));
        value.setSourceId(number(row.get("source_id")));
        value.setPointId(number(row.get("point_id")));
        value.setAlarmTypeFlag((int) number(row.get("alarm_type_flag")));
        value.setConfirmFlag((int) number(row.get("confirm_flag")));
        value.setCreateTime(time(row.get("create_time")));
        value.setMessage(text(row.get("message")));
        return value;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "a.create_time DESC, a.id DESC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column =
                    switch (spec.field()) {
                        case "id" -> "a.id";
                        case "sourceId" -> "a.entity_id";
                        case "pointId" -> "a.point_id";
                        case "alarmTypeFlag" -> "a.alarm_type_flag";
                        case "confirmFlag" -> "a.confirm_flag";
                        case "createTime" -> "a.create_time";
                        default ->
                            throw new IllegalArgumentException("alert sort field is not allowed: " + spec.field());
                    };
            clauses.add(column + (spec.direction() == SortSpec.Direction.ASC ? " ASC" : " DESC"));
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("a.id "))) clauses.add("a.id DESC");
        return String.join(", ", clauses);
    }

    private String messageExpression() {
        return switch (dialect.name().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "COALESCE(JSON_UNQUOTE(JSON_EXTRACT(a.alarm_ext, '$.content')), '')";
            default -> "COALESCE(a.alarm_ext ->> 'content', '')";
        };
    }

    private int sourceIndex(String source) {
        return switch (source) {
            case "point" -> 0;
            case "device" -> 1;
            case "driver" -> 2;
            default -> throw new IllegalArgumentException("alert source is not allowed: " + source);
        };
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private String text(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return null;
    }

    private boolean valid(Long value) {
        return value != null && value > 0;
    }
}
