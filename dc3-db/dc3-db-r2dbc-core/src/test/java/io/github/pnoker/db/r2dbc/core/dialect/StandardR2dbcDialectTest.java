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
package io.github.pnoker.db.r2dbc.core.dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class StandardR2dbcDialectTest {

    private final StandardR2dbcDialect dialect = new StandardR2dbcDialect("postgres", "public.fingerprint");

    @Test
    void quotesEveryQualifiedIdentifierSegment() {
        assertEquals("\"dc3_auth\".\"dc3_role\"", dialect.quoteIdentifier("dc3_auth.dc3_role"));
    }

    @Test
    void rejectsIdentifiersAndParametersThatCouldInjectSql() {
        assertThrows(IllegalArgumentException.class, () -> dialect.quoteIdentifier("role;drop"));
        assertThrows(IllegalArgumentException.class, () -> dialect.jsonParameter(":payload::json"));
    }

    @Test
    void bindsInstantUnchangedForTimestamptzColumns() {
        Instant value = Instant.parse("2026-08-28T01:02:03.123456Z");

        assertEquals(value, dialect.bindInstant(value));
    }

    @Test
    void emitsJsonbWriteAndTextExpressions() {
        assertEquals("CAST(:payload AS JSONB)", dialect.jsonWriteExpression(":payload"));
        assertEquals("ext#>>'{device,name}'", dialect.jsonTextExpression("ext", "device.name"));
    }

    @Test
    void emitsPortableCaseInsensitiveLikePredicate() {
        assertEquals("LOWER(name) LIKE LOWER(:name)", dialect.caseInsensitiveLike("name", ":name"));
    }
}
