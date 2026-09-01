package io.github.pnoker.db.r2dbc.core.dialect;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record StandardR2dbcDialect(
        String name,
        String schemaFingerprintTable,
        char identifierQuote,
        boolean supportsInsertReturning) implements R2dbcDialect {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public StandardR2dbcDialect {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("dialect name must not be blank");
        }
        if (schemaFingerprintTable == null || schemaFingerprintTable.isBlank()) {
            throw new IllegalArgumentException("schema fingerprint table must not be blank");
        }
        if (identifierQuote != '"' && identifierQuote != '`') {
            throw new IllegalArgumentException("unsupported identifier quote");
        }
    }

    @Override
    public String quoteIdentifier(String identifier) {
        Objects.requireNonNull(identifier, "identifier must not be null");
        return Arrays.stream(identifier.split("\\.", -1))
                .peek(part -> {
                    if (!IDENTIFIER.matcher(part).matches()) {
                        throw new IllegalArgumentException("invalid SQL identifier");
                    }
                })
                .map(part -> identifierQuote + part + identifierQuote)
                .collect(Collectors.joining("."));
    }

    @Override
    public String jsonWriteExpression(String namedParameter) {
        // PostgreSQL stores canonical structured payloads as JSONB; MySQL/MariaDB
        // accept the text value directly and validate it against their JSON type.
        return identifierQuote == '`' ? jsonParameter(namedParameter) : "CAST(" + jsonParameter(namedParameter) + " AS JSONB)";
    }

    @Override
    public String jsonTextExpression(String column, String path) {
        if (identifierQuote == '`') {
            return "JSON_UNQUOTE(JSON_EXTRACT(" + column + ",'$." + path + "'))";
        }
        String pgPath = String.join(",", path.split("\\.", -1)).replace("'", "''");
        return column + "#>>'{" + pgPath + "}'";
    }

    @Override
    public String jsonParameter(String namedParameter) {
        if (namedParameter == null || !namedParameter.matches(":[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("invalid named parameter");
        }
        return namedParameter;
    }

    @Override
    public Object bindInstant(Instant instant) {
        Objects.requireNonNull(instant, "instant must not be null");
        return identifierQuote == '`' ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC) : instant;
    }
}
