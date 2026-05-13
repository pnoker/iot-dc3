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

package io.github.pnoker.test.contracts;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reusable contract that asserts secret-bearing fields never leak through Lombok
 * generated {@code toString()} methods or are surfaced unintentionally on response
 * VOs. The default sensitive-field set covers the names called out by the AGENTS.md
 * domain modelling guidelines and can be extended via {@link #sensitiveFields()}.
 *
 * <p>Subclasses declare the VO classes they care about via {@link #voClasses()}.
 */
public abstract class SecretFieldContractTest {

    private static final Set<String> DEFAULT_SENSITIVE = Set.of(
            "apikey", "password", "secret", "token", "loginpassword", "saltpassword");

    private static Object newInstance(Class<?> type) throws ReflectiveOperationException {
        return type.getDeclaredConstructor().newInstance();
    }

    private static void populateStringFields(Object instance, Set<String> blocklist) {
        Class<?> current = instance.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() != String.class) {
                    continue;
                }
                if (!blocklist.contains(field.getName().toLowerCase())) {
                    continue;
                }
                field.setAccessible(true);
                Stream.of(field).forEach(f -> {
                    try {
                        f.set(instance, "should-not-leak");
                    } catch (IllegalAccessException ignored) {
                        // best-effort assignment; missing setter does not break the contract
                    }
                });
            }
            current = current.getSuperclass();
        }
        Arrays.stream(instance.getClass().getDeclaredFields()).forEach(field -> {
        });
    }

    protected abstract List<Class<?>> voClasses();

    protected Set<String> sensitiveFields() {
        return DEFAULT_SENSITIVE;
    }

    @Test
    void sensitiveFieldsMustBeAbsentFromToString() throws Exception {
        Set<String> blocklist = new LinkedHashSet<>();
        sensitiveFields().forEach(field -> blocklist.add(field.toLowerCase()));

        for (Class<?> voClass : voClasses()) {
            Object instance = newInstance(voClass);
            populateStringFields(instance, blocklist);
            String rendered = String.valueOf(instance).toLowerCase();
            for (String forbidden : blocklist) {
                assertThat(rendered)
                        .as("toString() of %s must not contain field name %s",
                                voClass.getSimpleName(), forbidden)
                        .doesNotContain(forbidden + "=");
            }
        }
    }
}
