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

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** PostgreSQL conventions for the SQL fragments that cannot be expressed portably. */
public record StandardR2dbcDialect(String name, String schemaFingerprintTable) implements R2dbcDialect {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public StandardR2dbcDialect {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("dialect name must not be blank");
        }
        if (schemaFingerprintTable == null || schemaFingerprintTable.isBlank()) {
            throw new IllegalArgumentException("schema fingerprint table must not be blank");
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
                .map(part -> '"' + part + '"')
                .collect(Collectors.joining("."));
    }

    @Override
    public String jsonParameter(String namedParameter) {
        if (namedParameter == null || !namedParameter.matches(":[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("invalid named parameter");
        }
        return namedParameter;
    }

    @Override
    public String jsonTextExpression(String column, String path) {
        String pgPath = String.join(",", path.split("\\.", -1)).replace("'", "''");
        return column + "#>>'{" + pgPath + "}'";
    }
}
