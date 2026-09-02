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

import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.runtime.config.R2dbcRuntimeProperties;
import io.r2dbc.spi.ConnectionFactory;
import java.util.Objects;
import reactor.core.publisher.Mono;

/** Verifies the clean DDL fingerprint before a center accepts traffic. */
public final class SchemaFingerprintVerifier {

    public static final String SCHEMA_CONTRACT = "r2dbc-flag-day-v1";
    /**
     * Business aggregate ids are BIGINT values generated from UUIDv7 via
     * {@code UuidV7.nextLong()}; operation, message and cursor ids use native
     * UUIDv7 columns.
     */
    public static final String ID_FORMAT = "uuidv7-bigint";

    public static final String TIME_FORMAT = "utc-micros";
    public static final String JSON_FORMAT = "canonical-v1";

    private final ConnectionFactory connectionFactory;
    private final R2dbcRuntimeProperties properties;
    private final R2dbcDialect dialect;

    public SchemaFingerprintVerifier(
            ConnectionFactory connectionFactory, R2dbcRuntimeProperties properties, R2dbcDialect dialect) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.dialect = Objects.requireNonNull(dialect, "dialect must not be null");
    }

    public Mono<Void> verify() {
        String expected = properties.getSchemaFingerprint();
        if (expected == null || !expected.matches("[0-9a-fA-F]{64}")) {
            return Mono.error(new IllegalStateException("dc3.r2dbc.schema-fingerprint must be configured"));
        }
        if (!SCHEMA_CONTRACT.equals(properties.getSchemaContract())
                || !ID_FORMAT.equals(properties.getIdFormat())
                || !TIME_FORMAT.equals(properties.getTimeFormat())
                || !JSON_FORMAT.equals(properties.getJsonFormat())) {
            return Mono.error(
                    new IllegalStateException("dc3.r2dbc data format properties must use the canonical contract"));
        }
        String table = dialect.quoteIdentifier(dialect.schemaFingerprintTable());
        return Mono.usingWhen(
                Mono.from(connectionFactory.create()),
                connection -> Mono.from(connection
                                .createStatement(
                                        "SELECT ddl_hash, schema_contract, id_format, time_format, json_format FROM "
                                                + table + " WHERE fingerprint_version = 2")
                                .execute())
                        .flatMap(result -> Mono.from(result.map((row, metadata) -> new Fingerprint(
                                row.get("ddl_hash", String.class),
                                row.get("schema_contract", String.class),
                                row.get("id_format", String.class),
                                row.get("time_format", String.class),
                                row.get("json_format", String.class)))))
                        .switchIfEmpty(Mono.error(new IllegalStateException("dc3_schema_fingerprint row is missing")))
                        .flatMap(actual -> {
                            if (!Objects.equals(properties.getSchemaContract(), actual.schemaContract())) {
                                return Mono.error(new IllegalStateException(
                                        "unsupported schema contract: " + actual.schemaContract()));
                            }
                            if (!Objects.equals(properties.getIdFormat(), actual.idFormat())
                                    || !Objects.equals(properties.getTimeFormat(), actual.timeFormat())
                                    || !Objects.equals(properties.getJsonFormat(), actual.jsonFormat())) {
                                return Mono.error(new IllegalStateException("unsupported data format: id="
                                        + actual.idFormat() + ", time=" + actual.timeFormat() + ", json="
                                        + actual.jsonFormat()));
                            }
                            return expected.equals(actual.ddlHash())
                                    ? Mono.empty()
                                    : Mono.error(new IllegalStateException("schema fingerprint mismatch: expected "
                                            + expected + ", actual " + actual.ddlHash()));
                        }),
                connection -> Mono.from(connection.close()),
                (connection, error) -> Mono.from(connection.close()),
                connection -> Mono.from(connection.close()));
    }

    private record Fingerprint(
            String ddlHash, String schemaContract, String idFormat, String timeFormat, String jsonFormat) {}
}
