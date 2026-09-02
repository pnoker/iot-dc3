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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.pnoker.db.r2dbc.core.page.CursorRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class R2dbcCursorExecutorTest {

    @Test
    void fetchesOneExtraRowAndBuildsNextCursorOnlyWhenTruncated() {
        DatabaseClient client = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec statement = mock(DatabaseClient.GenericExecuteSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<String> rows = mock(RowsFetchSpec.class);
        TransactionalOperator operator = mock(TransactionalOperator.class);
        when(client.sql("ITEMS")).thenReturn(statement);
        when(statement.bind("cursor", "before")).thenReturn(statement);
        when(statement.bind("limit", 3)).thenReturn(statement);
        when(statement.<String>map(
                        (java.util.function.BiFunction<io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata, String>) any()))
                .thenReturn(rows);
        when(rows.all()).thenReturn(Flux.just("a", "b", "c"));
        when(operator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(new R2dbcCursorExecutor(client, operator)
                        .execute(
                                new CursorRequest("before", 2),
                                "ITEMS",
                                Map.of(),
                                (row, metadata) -> "x",
                                value -> "next"))
                .assertNext(page -> {
                    org.junit.jupiter.api.Assertions.assertEquals(List.of("a", "b"), page.items());
                    org.junit.jupiter.api.Assertions.assertTrue(page.hasNext());
                    org.junit.jupiter.api.Assertions.assertEquals("next", page.nextCursor());
                })
                .verifyComplete();
    }

    @Test
    void rejectsARepositoryThatOverridesTheReservedLimit() {
        DatabaseClient client = mock(DatabaseClient.class);
        TransactionalOperator operator = mock(TransactionalOperator.class);

        StepVerifier.create(new R2dbcCursorExecutor(client, operator)
                        .execute(
                                CursorRequest.firstPage(),
                                "ITEMS",
                                Map.of("limit", 100),
                                (row, metadata) -> "x",
                                value -> "next"))
                .expectErrorMessage("cursor and limit are reserved pagination parameters")
                .verify();
    }

    @Test
    void bindsNullCursorForTheFirstPage() {
        DatabaseClient client = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec statement = mock(DatabaseClient.GenericExecuteSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<String> rows = mock(RowsFetchSpec.class);
        TransactionalOperator operator = mock(TransactionalOperator.class);
        when(client.sql("ITEMS")).thenReturn(statement);
        when(statement.bindNull("cursor", String.class)).thenReturn(statement);
        when(statement.bind("limit", 51)).thenReturn(statement);
        when(statement.<String>map(
                        (java.util.function.BiFunction<io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata, String>) any()))
                .thenReturn(rows);
        when(rows.all()).thenReturn(Flux.just("a"));
        when(operator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(new R2dbcCursorExecutor(client, operator)
                        .execute(CursorRequest.firstPage(), "ITEMS", Map.of(), (row, metadata) -> "x", value -> "next"))
                .assertNext(page -> org.junit.jupiter.api.Assertions.assertFalse(page.hasNext()))
                .verifyComplete();
    }
}
