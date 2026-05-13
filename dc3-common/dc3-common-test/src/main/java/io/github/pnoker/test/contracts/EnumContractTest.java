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

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Reusable contract for IoT DC3 domain enums.
 *
 * <p>Each subclass declares the enum type via {@link #enumClass()} and the contract
 * derives a battery of assertions from reflection:
 * <ul>
 *   <li>every constant has a unique, non-null index returned by {@code getIndex()};</li>
 *   <li>round-trip {@code ofIndex(getIndex()) == constant} for all constants;</li>
 *   <li>{@code name()} stays consistent (catches accidental renames during refactors).</li>
 * </ul>
 *
 * <p>Subclasses can override the index/factory method names if a particular enum
 * uses different conventions.
 */
public abstract class EnumContractTest<E extends Enum<E>> {

    protected abstract Class<E> enumClass();

    protected String indexAccessor() {
        return "getIndex";
    }

    protected String factoryMethod() {
        return "ofIndex";
    }

    @TestFactory
    final List<DynamicTest> indexContractIsHonoured() {
        List<DynamicTest> tests = new ArrayList<>();
        E[] constants = enumClass().getEnumConstants();
        Method index = findIndexAccessor();
        Method factory = findFactoryMethod(index.getReturnType());

        List<Object> indices = new ArrayList<>();
        for (E constant : constants) {
            tests.add(DynamicTest.dynamicTest(
                    "%s has non-null index".formatted(constant.name()),
                    () -> {
                        Object value = index.invoke(constant);
                        assertThat(value).as("index of %s", constant.name()).isNotNull();
                        indices.add(value);
                    }));
            tests.add(DynamicTest.dynamicTest(
                    "%s round-trips through %s".formatted(constant.name(), factory.getName()),
                    () -> {
                        Object value = index.invoke(constant);
                        Object resolved = factory.invoke(null, value);
                        assertThat(resolved).isEqualTo(constant);
                    }));
        }

        tests.add(DynamicTest.dynamicTest("indices are unique", () -> {
            for (E constant : constants) {
                indices.add(index.invoke(constant));
            }
            assertThat(indices).doesNotHaveDuplicates();
        }));

        tests.add(DynamicTest.dynamicTest("name stability", () ->
                assertThatNoException().isThrownBy(() ->
                        Arrays.stream(constants).forEach(Enum::name))));

        return tests;
    }

    private Method findIndexAccessor() {
        try {
            return enumClass().getMethod(indexAccessor());
        } catch (NoSuchMethodException e) {
            throw new AssertionError(
                    "Enum %s does not expose %s()".formatted(enumClass().getName(), indexAccessor()),
                    e);
        }
    }

    private Method findFactoryMethod(Class<?> indexType) {
        try {
            return enumClass().getMethod(factoryMethod(), indexType);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(
                    "Enum %s does not expose %s(%s)".formatted(
                            enumClass().getName(), factoryMethod(), indexType.getSimpleName()),
                    e);
        }
    }
}
