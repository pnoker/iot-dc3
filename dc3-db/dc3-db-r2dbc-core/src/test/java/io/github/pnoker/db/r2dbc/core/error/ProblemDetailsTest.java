package io.github.pnoker.db.r2dbc.core.error;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProblemDetailsTest {

    @Test
    void defensivelyCopiesValidationErrors() {
        List<String> messages = new ArrayList<>(List.of("required"));
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", messages);

        ProblemDetails details = new ProblemDetails(null, "Validation failed", 422,
                "validation_failed", "Invalid input", null, null, errors);
        messages.add("changed");
        errors.put("other", List.of("leaked"));

        assertEquals(List.of("required"), details.errors().get("name"));
        assertThrows(UnsupportedOperationException.class,
                () -> details.errors().get("name").add("mutate"));
        assertThrows(UnsupportedOperationException.class,
                () -> details.errors().put("other", List.of("mutate")));
    }
}
