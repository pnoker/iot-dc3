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
package io.github.pnoker.db.r2dbc.core.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProblemDetailsTest {

    @Test
    void defensivelyCopiesValidationErrors() {
        List<String> messages = new ArrayList<>(List.of("required"));
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", messages);

        ProblemDetails details = new ProblemDetails(
                null, "Validation failed", 422, "validation_failed", "Invalid input", null, null, errors);
        messages.add("changed");
        errors.put("other", List.of("leaked"));

        assertEquals(List.of("required"), details.errors().get("name"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> details.errors().get("name").add("mutate"));
        assertThrows(UnsupportedOperationException.class, () -> details.errors().put("other", List.of("mutate")));
    }
}
