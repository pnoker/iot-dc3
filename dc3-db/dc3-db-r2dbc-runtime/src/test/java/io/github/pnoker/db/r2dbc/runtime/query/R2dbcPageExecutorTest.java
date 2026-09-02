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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class R2dbcPageExecutorTest {

    @Test
    void bindsPageParametersAndBuildsCanonicalResult() {
        DatabaseClient client = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec countStatement = mock(DatabaseClient.GenericExecuteSpec.class);
        DatabaseClient.GenericExecuteSpec itemStatement = mock(DatabaseClient.GenericExecuteSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<Long> countRows = mock(RowsFetchSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<String> itemRows = mock(RowsFetchSpec.class);
        PageTransaction pageTransaction = mock(PageTransaction.class);

        when(client.sql("SELECT count(*)")).thenReturn(countStatement);
        when(client.sql("SELECT name")).thenReturn(itemStatement);
        when(countStatement.bind("tenant", 7L)).thenReturn(countStatement);
        when(countStatement.mapValue(Long.class)).thenReturn(countRows);
        when(countRows.one()).thenReturn(Mono.just(3L));
        when(itemStatement.bind("tenant", 7L)).thenReturn(itemStatement);
        when(itemStatement.bind("offset", 2L)).thenReturn(itemStatement);
        when(itemStatement.bind("limit", 2)).thenReturn(itemStatement);
        @SuppressWarnings("unchecked")
        DatabaseClient.GenericExecuteSpec mapped = itemStatement;
        when(mapped.<String>map(
                        (java.util.function.BiFunction<io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata, String>) any()))
                .thenReturn(itemRows);
        when(itemRows.all()).thenReturn(Flux.just("a"));
        when(pageTransaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(new R2dbcPageExecutor(client, pageTransaction)
                        .execute(
                                new PageRequest(2, 2),
                                "SELECT count(*)",
                                "SELECT name",
                                Map.of("tenant", 7L),
                                (row, metadata) -> row.get("name", String.class)))
                .assertNext(page -> {
                    org.junit.jupiter.api.Assertions.assertEquals(List.of("a"), page.items());
                    org.junit.jupiter.api.Assertions.assertEquals(3L, page.total());
                    org.junit.jupiter.api.Assertions.assertFalse(page.hasNext());
                })
                .verifyComplete();

        verify(itemStatement).bind("offset", 2L);
        verify(itemStatement).bind("limit", 2);
    }

    @Test
    void rejectsAnInvalidParameterNameBeforeQueryExecution() {
        DatabaseClient client = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec statement = mock(DatabaseClient.GenericExecuteSpec.class);
        PageTransaction pageTransaction = mock(PageTransaction.class);
        doReturn(statement).when(client).sql("SELECT count(*)");
        when(pageTransaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(new R2dbcPageExecutor(client, pageTransaction)
                        .execute(
                                PageRequest.firstPage(),
                                "SELECT count(*)",
                                "SELECT name",
                                Map.of("tenant;drop", 1L),
                                (row, metadata) -> "x"))
                .expectErrorMessage("invalid SQL parameter name")
                .verify();
    }

    @Test
    void rejectsReservedPaginationParametersFromRepositoryFilters() {
        DatabaseClient client = mock(DatabaseClient.class);
        PageTransaction pageTransaction = mock(PageTransaction.class);

        StepVerifier.create(new R2dbcPageExecutor(client, pageTransaction)
                        .execute(
                                PageRequest.firstPage(),
                                "COUNT",
                                "ITEMS",
                                Map.of("offset", 99L),
                                (row, metadata) -> "x"))
                .expectErrorMessage("offset and limit are reserved pagination parameters")
                .verify();
    }

    @Test
    void bindsNullParametersWithoutUsingMapCopyOf() {
        DatabaseClient client = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec countStatement = mock(DatabaseClient.GenericExecuteSpec.class);
        DatabaseClient.GenericExecuteSpec itemStatement = mock(DatabaseClient.GenericExecuteSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<Long> countRows = mock(RowsFetchSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<String> itemRows = mock(RowsFetchSpec.class);
        PageTransaction pageTransaction = mock(PageTransaction.class);

        when(client.sql("COUNT")).thenReturn(countStatement);
        when(client.sql("ITEMS")).thenReturn(itemStatement);
        when(countStatement.bindNull("optional", Object.class)).thenReturn(countStatement);
        when(countStatement.mapValue(Long.class)).thenReturn(countRows);
        when(countRows.one()).thenReturn(Mono.just(0L));
        when(itemStatement.bindNull("optional", Object.class)).thenReturn(itemStatement);
        when(itemStatement.bind("offset", 0L)).thenReturn(itemStatement);
        when(itemStatement.bind("limit", 5)).thenReturn(itemStatement);
        when(itemStatement.<String>map(
                        (java.util.function.BiFunction<io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata, String>) any()))
                .thenReturn(itemRows);
        when(itemRows.all()).thenReturn(Flux.empty());
        when(pageTransaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("optional", null);
        StepVerifier.create(new R2dbcPageExecutor(client, pageTransaction)
                        .execute(new PageRequest(0, 5), "COUNT", "ITEMS", parameters, (row, metadata) -> "x"))
                .assertNext(page -> org.junit.jupiter.api.Assertions.assertFalse(page.hasNext()))
                .verifyComplete();

        verify(countStatement).bindNull("optional", Object.class);
        verify(itemStatement).bindNull("optional", Object.class);
    }

    @Test
    void keepsCountAndItemsInsideTheConfiguredReactiveTransaction() {
        DatabaseClient client = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec countStatement = mock(DatabaseClient.GenericExecuteSpec.class);
        DatabaseClient.GenericExecuteSpec itemStatement = mock(DatabaseClient.GenericExecuteSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<Long> countRows = mock(RowsFetchSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<String> itemRows = mock(RowsFetchSpec.class);
        PageTransaction pageTransaction = mock(PageTransaction.class);

        when(client.sql("COUNT")).thenReturn(countStatement);
        when(client.sql("ITEMS")).thenReturn(itemStatement);
        when(countStatement.mapValue(Long.class)).thenReturn(countRows);
        when(countRows.one()).thenReturn(Mono.just(1L));
        when(itemStatement.bind("offset", 0L)).thenReturn(itemStatement);
        when(itemStatement.bind("limit", 5)).thenReturn(itemStatement);
        when(itemStatement.<String>map(
                        (java.util.function.BiFunction<io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata, String>) any()))
                .thenReturn(itemRows);
        when(itemRows.all()).thenReturn(Flux.just("one"));
        when(pageTransaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(new R2dbcPageExecutor(client, pageTransaction)
                        .execute(new PageRequest(0, 5), "COUNT", "ITEMS", Map.of(), (row, metadata) -> "one"))
                .assertNext(page -> org.junit.jupiter.api.Assertions.assertEquals(List.of("one"), page.items()))
                .verifyComplete();

        verify(pageTransaction).transactional(any(Mono.class));
    }
}
