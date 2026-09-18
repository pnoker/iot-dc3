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
package io.github.pnoker.db.r2dbc.runtime.schema;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.db.r2dbc.core.dialect.StandardR2dbcDialect;
import io.github.pnoker.db.r2dbc.runtime.config.R2dbcRuntimeProperties;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class SchemaFingerprintVerifierTest {

    @Test
    void failsBeforeOpeningAConnectionWhenFingerprintIsMissing() {
        R2dbcRuntimeProperties properties = new R2dbcRuntimeProperties();
        SchemaFingerprintVerifier verifier = new SchemaFingerprintVerifier(
                mock(ConnectionFactory.class),
                properties,
                new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint"));

        StepVerifier.create(verifier.verify())
                .expectErrorMessage("dc3.r2dbc.schema-fingerprint must be configured")
                .verify();
    }

    @Test
    void quotesTheControlledFingerprintTableIdentifier() {
        R2dbcRuntimeProperties properties = new R2dbcRuntimeProperties();
        properties.setSchemaFingerprint("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        io.r2dbc.spi.Connection connection = mock(io.r2dbc.spi.Connection.class);
        io.r2dbc.spi.Statement statement = mock(io.r2dbc.spi.Statement.class);
        doReturn(Flux.just(connection)).when(connectionFactory).create();
        when(connection.createStatement(contains("\"public\".\"dc3_schema_fingerprint\"")))
                .thenReturn(statement);
        doReturn(Flux.empty()).when(statement).execute();
        doReturn(Mono.empty()).when(connection).close();

        SchemaFingerprintVerifier verifier = new SchemaFingerprintVerifier(
                connectionFactory, properties, new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint"));

        StepVerifier.create(verifier.verify())
                .expectErrorMessage("dc3_schema_fingerprint row is missing")
                .verify();
        verify(connection).close();
    }

    @Test
    void rejectsAnUnsupportedSchemaContract() {
        R2dbcRuntimeProperties properties = new R2dbcRuntimeProperties();
        properties.setSchemaFingerprint("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        io.r2dbc.spi.Connection connection = mock(io.r2dbc.spi.Connection.class);
        Statement statement = mock(Statement.class);
        Result result = mock(Result.class);
        Row row = mock(Row.class);
        RowMetadata metadata = mock(RowMetadata.class);
        doReturn(Flux.just(connection)).when(connectionFactory).create();
        doReturn(statement).when(connection).createStatement(contains("fingerprint_version = 2"));
        doReturn(Flux.just(result)).when(statement).execute();
        when(row.get("ddl_hash", String.class))
                .thenReturn("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        when(row.get("schema_contract", String.class)).thenReturn("old-contract");
        when(row.get("id_format", String.class)).thenReturn("uuidv7-bigint");
        when(row.get("time_format", String.class)).thenReturn("utc-micros");
        when(row.get("json_format", String.class)).thenReturn("canonical-v1");
        when(result.map(any(java.util.function.BiFunction.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.BiFunction<Row, RowMetadata, Object> mapper = invocation.getArgument(0);
            return Flux.just(mapper.apply(row, metadata));
        });
        doReturn(Mono.empty()).when(connection).close();

        SchemaFingerprintVerifier verifier = new SchemaFingerprintVerifier(
                connectionFactory, properties, new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint"));

        StepVerifier.create(verifier.verify())
                .expectErrorMessage("unsupported schema contract: old-contract")
                .verify();
        verify(connection).close();
    }

    @Test
    void rejectsAnUnsupportedDataFormat() {
        R2dbcRuntimeProperties properties = new R2dbcRuntimeProperties();
        properties.setSchemaFingerprint("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        io.r2dbc.spi.Connection connection = mock(io.r2dbc.spi.Connection.class);
        Statement statement = mock(Statement.class);
        Result result = mock(Result.class);
        Row row = mock(Row.class);
        RowMetadata metadata = mock(RowMetadata.class);
        doReturn(Flux.just(connection)).when(connectionFactory).create();
        doReturn(statement).when(connection).createStatement(contains("fingerprint_version = 2"));
        doReturn(Flux.just(result)).when(statement).execute();
        when(row.get("ddl_hash", String.class))
                .thenReturn("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        when(row.get("schema_contract", String.class)).thenReturn("r2dbc-flag-day-v1");
        when(row.get("id_format", String.class)).thenReturn("long");
        when(row.get("time_format", String.class)).thenReturn("utc-micros");
        when(row.get("json_format", String.class)).thenReturn("canonical-v1");
        when(result.map(any(java.util.function.BiFunction.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.BiFunction<Row, RowMetadata, Object> mapper = invocation.getArgument(0);
            return Flux.just(mapper.apply(row, metadata));
        });
        doReturn(Mono.empty()).when(connection).close();

        SchemaFingerprintVerifier verifier = new SchemaFingerprintVerifier(
                connectionFactory, properties, new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint"));

        StepVerifier.create(verifier.verify())
                .expectErrorMessage("unsupported data format: id=long, time=utc-micros, json=canonical-v1")
                .verify();
        verify(connection).close();
    }
}
