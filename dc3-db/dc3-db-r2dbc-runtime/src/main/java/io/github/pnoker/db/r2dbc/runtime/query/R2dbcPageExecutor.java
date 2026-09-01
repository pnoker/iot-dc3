package io.github.pnoker.db.r2dbc.runtime.query;

import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.function.BiFunction;

/**
 * Executes the two statements required by an offset page.
 *
 * <p>The SQL is supplied by a trusted repository adapter. Client-controlled
 * values are only ever passed through named bind markers; repositories remain
 * responsible for tenant predicates and sort-whitelist validation.</p>
 */
public final class R2dbcPageExecutor {

    private final DatabaseClient databaseClient;
    private final PageTransaction pageTransaction;

    public R2dbcPageExecutor(DatabaseClient databaseClient, PageTransaction pageTransaction) {
        this.databaseClient = Objects.requireNonNull(databaseClient, "databaseClient must not be null");
        this.pageTransaction = Objects.requireNonNull(pageTransaction, "pageTransaction must not be null");
    }

    public <T> Mono<OffsetPage<T>> execute(
            PageRequest pageRequest,
            String countSql,
            String itemsSql,
            Map<String, ?> parameters,
            BiFunction<io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata, T> rowMapper) {
        return Mono.defer(() -> {
            Objects.requireNonNull(pageRequest, "pageRequest must not be null");
            requireSql(countSql, "countSql");
            requireSql(itemsSql, "itemsSql");
            Objects.requireNonNull(rowMapper, "rowMapper must not be null");
            Map<String, ?> safeParameters = parameters == null
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
            if (safeParameters.containsKey("offset") || safeParameters.containsKey("limit")) {
                return Mono.error(new IllegalArgumentException("offset and limit are reserved pagination parameters"));
            }

            Mono<OffsetPage<T>> page = bind(databaseClient.sql(countSql), safeParameters)
                    .mapValue(Long.class)
                    .one()
                    .switchIfEmpty(Mono.error(new IllegalStateException("count query returned no row")))
                    .flatMap(total -> bind(databaseClient.sql(itemsSql), safeParameters)
                            .bind("offset", pageRequest.offset())
                            .bind("limit", pageRequest.limit())
                            .map(rowMapper)
                            .all()
                            .collectList()
                            .map(items -> OffsetPage.of(items, pageRequest.offset(), pageRequest.limit(), total)));
            return pageTransaction.transactional(page);
        });
    }

    static DatabaseClient.GenericExecuteSpec bind(
            DatabaseClient.GenericExecuteSpec statement, Map<String, ?> parameters) {
        DatabaseClient.GenericExecuteSpec bound = statement;
        for (Map.Entry<String, ?> entry : parameters.entrySet()) {
            String name = entry.getKey();
            if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("invalid SQL parameter name");
            }
            Object value = entry.getValue();
            bound = value == null
                    ? bound.bindNull(name, Object.class)
                    : bound.bind(name, value);
        }
        return bound;
    }

    static void requireSql(String sql, String name) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
