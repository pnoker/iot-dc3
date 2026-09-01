package io.github.pnoker.db.r2dbc.core.dialect;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StandardR2dbcDialectTest {

    @Test
    void quotesEveryQualifiedIdentifierSegment() {
        StandardR2dbcDialect dialect = new StandardR2dbcDialect("postgres", "public.fingerprint", '"', true);

        assertEquals("\"dc3_auth\".\"dc3_role\"", dialect.quoteIdentifier("dc3_auth.dc3_role"));
    }

    @Test
    void rejectsIdentifiersAndParametersThatCouldInjectSql() {
        StandardR2dbcDialect dialect = new StandardR2dbcDialect("mysql", "dc3_runtime.fingerprint", '`', false);

        assertThrows(IllegalArgumentException.class, () -> dialect.quoteIdentifier("role;drop"));
        assertThrows(IllegalArgumentException.class, () -> dialect.jsonParameter(":payload::json"));
    }

    @Test
    void bindsPostgresInstantAndMysqlUtcDateTimeWithoutTimezoneDrift() {
        Instant value = Instant.parse("2026-08-28T01:02:03.123456Z");
        StandardR2dbcDialect postgres = new StandardR2dbcDialect("postgres", "public.fingerprint", '"', true);
        StandardR2dbcDialect mysql = new StandardR2dbcDialect("mysql", "dc3_runtime.fingerprint", '`', false);

        assertEquals(value, postgres.bindInstant(value));
        assertEquals(LocalDateTime.of(2026, 8, 28, 1, 2, 3, 123456000), mysql.bindInstant(value));
    }

    @Test
    void emitsDatabaseSpecificJsonWriteExpressions() {
        StandardR2dbcDialect postgres = new StandardR2dbcDialect("postgres", "public.fingerprint", '"', true);
        StandardR2dbcDialect mysql = new StandardR2dbcDialect("mysql", "dc3_runtime.fingerprint", '`', false);

        assertEquals("CAST(:payload AS JSONB)", postgres.jsonWriteExpression(":payload"));
        assertEquals(":payload", mysql.jsonWriteExpression(":payload"));
    }

    @Test
    void emitsPortableCaseInsensitiveLikePredicate() {
        StandardR2dbcDialect postgres = new StandardR2dbcDialect("postgres", "public.fingerprint", '"', true);
        StandardR2dbcDialect mysql = new StandardR2dbcDialect("mysql", "dc3_runtime.fingerprint", '`', false);

        assertEquals("LOWER(name) LIKE LOWER(:name)", postgres.caseInsensitiveLike("name", ":name"));
        assertEquals("LOWER(name) LIKE LOWER(:name)", mysql.caseInsensitiveLike("name", ":name"));
    }
}
