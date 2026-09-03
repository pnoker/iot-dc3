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
package io.github.pnoker.db.r2dbc.runtime.query;

import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import io.github.pnoker.db.r2dbc.core.page.CursorRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

/** Executes a bounded keyset page without exposing an unbounded result stream. */
public final class R2dbcCursorExecutor {

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;

    public R2dbcCursorExecutor(DatabaseClient databaseClient, TransactionalOperator transactionalOperator) {
        this.databaseClient = Objects.requireNonNull(databaseClient, "databaseClient must not be null");
        this.transactionalOperator =
                Objects.requireNonNull(transactionalOperator, "transactionalOperator must not be null");
    }

    /** Run the cursor query inside the transaction, emitting the page. */
    public <T> Mono<CursorPage<T>> execute(
            CursorRequest request,
            String itemsSql,
            Map<String, ?> parameters,
            BiFunction<io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata, T> rowMapper,
            Function<? super T, String> nextCursor) {
        return Mono.defer(() -> {
            Objects.requireNonNull(request, "request must not be null");
            R2dbcPageExecutor.requireSql(itemsSql, "itemsSql");
            Objects.requireNonNull(rowMapper, "rowMapper must not be null");
            Objects.requireNonNull(nextCursor, "nextCursor must not be null");
            Map<String, ?> safeParameters =
                    parameters == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
            if (safeParameters.containsKey("limit") || safeParameters.containsKey("cursor")) {
                return Mono.error(new IllegalArgumentException("cursor and limit are reserved pagination parameters"));
            }

            DatabaseClient.GenericExecuteSpec statement =
                    R2dbcPageExecutor.bind(databaseClient.sql(itemsSql), safeParameters);
            statement = request.cursor() == null
                    ? statement.bindNull("cursor", String.class)
                    : statement.bind("cursor", request.cursor());
            Mono<CursorPage<T>> page = statement
                    .bind("limit", request.limit() + 1)
                    .map(rowMapper)
                    .all()
                    .collectList()
                    .flatMap(rows -> page(rows, request.limit(), nextCursor));
            return transactionalOperator.transactional(page);
        });
    }

    private static <T> Mono<CursorPage<T>> page(List<T> rows, int limit, Function<? super T, String> nextCursor) {
        boolean hasNext = rows.size() > limit;
        List<T> items = hasNext ? new ArrayList<>(rows.subList(0, limit)) : rows;
        if (!hasNext) {
            return Mono.just(new CursorPage<>(items, null, false));
        }
        String cursor = nextCursor.apply(items.get(items.size() - 1));
        if (cursor == null || cursor.isBlank()) {
            return Mono.error(new IllegalStateException("next cursor is required when the result is truncated"));
        }
        return Mono.just(new CursorPage<>(items, cursor, true));
    }
}
