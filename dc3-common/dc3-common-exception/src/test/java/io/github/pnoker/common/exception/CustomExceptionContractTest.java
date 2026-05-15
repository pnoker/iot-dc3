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

package io.github.pnoker.common.exception;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reusable contract for the dc3-common-exception family. Every custom exception
 * exposes the same triple of constructors (no-arg, cause, template+params). The
 * contract verifies that each constructor sets the expected runtime exception
 * state without exercising every class in a separate file.
 */
class CustomExceptionContractTest {

    private static final List<Class<? extends RuntimeException>> EXCEPTIONS = List.of(
            AddException.class,
            AssociatedException.class,
            ConfigException.class,
            ConnectorException.class,
            CronException.class,
            DeleteException.class,
            DuplicateException.class,
            EmptyException.class,
            ImportException.class,
            JsonException.class,
            NotFoundException.class,
            OutRangeException.class,
            ReadPointException.class,
            RegisterException.class,
            RepositoryException.class,
            RequestException.class,
            SecurityException.class,
            ServiceException.class,
            TypeException.class,
            UnAuthorizedException.class,
            UnSupportException.class,
            UpdateException.class,
            WritePointException.class);

    @TestFactory
    Stream<DynamicTest> exceptionsExposeAllThreeConstructorVariants() {
        return EXCEPTIONS.stream().flatMap(exceptionClass -> Stream.of(
                DynamicTest.dynamicTest(
                        exceptionClass.getSimpleName() + " no-arg leaves message and cause null",
                        () -> {
                            RuntimeException exception = exceptionClass.getDeclaredConstructor().newInstance();
                            assertThat(exception.getMessage()).isNull();
                            assertThat(exception.getCause()).isNull();
                        }),
                DynamicTest.dynamicTest(
                        exceptionClass.getSimpleName() + " cause constructor wires throwable",
                        () -> {
                            Throwable cause = new IllegalStateException("root");
                            RuntimeException exception = exceptionClass
                                    .getDeclaredConstructor(Throwable.class)
                                    .newInstance(cause);
                            assertThat(exception.getCause()).isSameAs(cause);
                        }),
                DynamicTest.dynamicTest(
                        exceptionClass.getSimpleName() + " template constructor formats placeholders",
                        () -> {
                            RuntimeException exception = exceptionClass
                                    .getDeclaredConstructor(String.class, Object[].class)
                                    .newInstance("hello {} number {}", new Object[]{"world", 42});
                            assertThat(exception.getMessage()).isEqualTo("hello world number 42");
                        }),
                DynamicTest.dynamicTest(
                        exceptionClass.getSimpleName() + " template constructor preserves trailing cause",
                        () -> {
                            Throwable cause = new IllegalStateException("root");
                            RuntimeException exception = exceptionClass
                                    .getDeclaredConstructor(String.class, Object[].class)
                                    .newInstance("hello {}", new Object[]{"world", cause});
                            assertThat(exception.getMessage()).isEqualTo("hello world");
                            assertThat(exception.getCause()).isSameAs(cause);
                        })));
    }
}
