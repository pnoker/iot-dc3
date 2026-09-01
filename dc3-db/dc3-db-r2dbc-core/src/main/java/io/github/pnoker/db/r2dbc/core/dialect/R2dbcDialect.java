package io.github.pnoker.db.r2dbc.core.dialect;

import java.time.Instant;

/** Database-specific SQL fragments that cannot be expressed portably. */
public interface R2dbcDialect {

    String name();

    String schemaFingerprintTable();

    /** Fully-qualified runtime operation table used by the durable async job port. */
    default String operationTable() {
        return schemaFingerprintTable().replace("dc3_schema_fingerprint", "dc3_operation");
    }

    default Object bindInstant(Instant instant) {
        return instant;
    }

    String quoteIdentifier(String identifier);

    String jsonParameter(String namedParameter);

    /** SQL expression used when writing a JSON value parameter. */
    default String jsonWriteExpression(String namedParameter) {
        return "CAST(" + jsonParameter(namedParameter) + " AS JSONB)";
    }

    /** SQL expression extracting a scalar text value from a JSON document. */
    default String jsonTextExpression(String column, String path) {
        return column + "->'" + path + "'";
    }

    /** Portable case-insensitive pattern predicate used by repository filters. */
    default String caseInsensitiveLike(String column, String parameter) {
        return "LOWER(" + column + ") LIKE LOWER(" + parameter + ")";
    }

    boolean supportsInsertReturning();
}
