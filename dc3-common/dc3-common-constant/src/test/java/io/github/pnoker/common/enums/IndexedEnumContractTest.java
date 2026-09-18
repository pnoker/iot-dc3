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
package io.github.pnoker.common.enums;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.common.constant.common.SymbolConstant;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Cross-cutting contract for every domain enum that uses the standard
 * {@code Byte index} + {@code getIndex()} + {@code ofIndex(Byte)} convention.
 *
 * <p>The contract verifies, for each enum constant:
 * <ul>
 *   <li>{@code getIndex()} returns a non-null {@link Byte}; the indices across all
 *       constants are unique;</li>
 *   <li>{@code ofIndex(constant.getIndex())} round-trips to the same constant;</li>
 *   <li>{@code ofIndex} of an index that does not match any constant returns
 *       {@code null} — required because the cache layer treats null as "unknown"
 *       and falls back to legacy behaviour rather than throwing.</li>
 * </ul>
 *
 * <p>Enums are discovered from the compiled {@code io.github.pnoker.common.enums}
 * package. New domain enums therefore enter the applicable contract automatically.
 */
class IndexedEnumContractTest {

    private static final String ENUM_PACKAGE = "io.github.pnoker.common.enums";
    private static final List<Class<? extends Enum<?>>> DOMAIN_ENUMS = discoverDomainEnums();
    private static final List<Class<? extends Enum<?>>> INDEXED_ENUMS =
            DOMAIN_ENUMS.stream().filter(type -> hasMethod(type, "getIndex")).toList();
    private static final List<Class<? extends Enum<?>>> CODED_ENUMS =
            DOMAIN_ENUMS.stream().filter(type -> hasMethod(type, "getCode")).toList();

    private static List<Class<? extends Enum<?>>> discoverDomainEnums() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String packagePath = ENUM_PACKAGE.replace('.', '/');
        List<Class<? extends Enum<?>>> enums = new ArrayList<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (!"file".equals(resource.getProtocol())) {
                    continue;
                }
                try (Stream<Path> classes = Files.list(Path.of(resource.toURI()))) {
                    classes.filter(path -> path.getFileName().toString().endsWith("Enum.class"))
                            .filter(path -> !path.getFileName().toString().contains("$"))
                            .forEach(path -> addEnumClass(classLoader, path, enums));
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to discover common domain enums", e);
        }
        if (enums.isEmpty()) {
            throw new IllegalStateException("No common domain enums discovered from " + packagePath);
        }
        enums.sort(Comparator.comparing(Class::getName));
        return List.copyOf(enums);
    }

    @SuppressWarnings("unchecked")
    private static void addEnumClass(ClassLoader classLoader, Path path, List<Class<? extends Enum<?>>> enums) {
        String fileName = path.getFileName().toString();
        String className =
                ENUM_PACKAGE + SymbolConstant.DOT + fileName.substring(0, fileName.length() - ".class".length());
        try {
            Class<?> candidate = Class.forName(className, false, classLoader);
            if (candidate.isEnum()) {
                enums.add((Class<? extends Enum<?>>) candidate);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load discovered enum " + className, e);
        }
    }

    private static boolean hasMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            type.getMethod(name, parameterTypes);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(
                    "Enum %s does not expose %s(%s)"
                            .formatted(
                                    type.getName(),
                                    name,
                                    String.join(
                                            ",",
                                            Stream.of(parameterTypes)
                                                    .map(Class::getSimpleName)
                                                    .toList())),
                    e);
        }
    }

    private static Byte unusedIndex(Enum<?>[] constants, Method getIndex) throws Exception {
        Set<Byte> taken = new HashSet<>();
        for (Enum<?> constant : constants) {
            taken.add((Byte) getIndex.invoke(constant));
        }
        // Search in the full Byte range so wide-coded enums still find a free slot.
        for (int candidate = -128; candidate <= 127; candidate++) {
            Byte byteCandidate = (byte) candidate;
            if (!taken.contains(byteCandidate)) {
                return byteCandidate;
            }
        }
        throw new IllegalStateException("No unused byte index available");
    }

    @TestFactory
    Stream<DynamicTest> indexedEnumsHonourTheStandardContract() {
        return INDEXED_ENUMS.stream().flatMap(this::contractFor);
    }

    @TestFactory
    Stream<DynamicTest> codedEnumsUseConsistentCodesAndRemarks() {
        return CODED_ENUMS.stream().flatMap(this::codeAndRemarkContractFor);
    }

    private Stream<DynamicTest> contractFor(Class<? extends Enum<?>> enumClass) {
        Method getIndex = method(enumClass, "getIndex");
        Method ofIndex = method(enumClass, "ofIndex", Byte.class);

        Enum<?>[] constants = enumClass.getEnumConstants();
        Set<Byte> seen = new HashSet<>();

        Stream<DynamicTest> perConstant = Stream.of(constants).flatMap(constant -> {
            String name = enumClass.getSimpleName() + SymbolConstant.DOT + constant.name();
            return Stream.of(
                    DynamicTest.dynamicTest(name + " has non-null index", () -> {
                        Byte index = (Byte) getIndex.invoke(constant);
                        assertThat(index).as(name + " getIndex").isNotNull();
                        assertThat(seen.add(index))
                                .as("%s index %s must be unique within %s", name, index, enumClass.getSimpleName())
                                .isTrue();
                    }),
                    DynamicTest.dynamicTest(name + " round-trips through ofIndex", () -> {
                        Byte index = (Byte) getIndex.invoke(constant);
                        Object resolved = ofIndex.invoke(null, index);
                        assertThat(resolved).isEqualTo(constant);
                    }));
        });

        Stream<DynamicTest> rejection = Stream.of(DynamicTest.dynamicTest(
                enumClass.getSimpleName() + ".ofIndex returns null for unknown index", () -> {
                    Byte unknown = unusedIndex(constants, getIndex);
                    assertThat(ofIndex.invoke(null, unknown)).isNull();
                }));

        return Stream.concat(perConstant, rejection);
    }

    private Stream<DynamicTest> codeAndRemarkContractFor(Class<? extends Enum<?>> enumClass) {
        Method getCode = method(enumClass, "getCode");
        Method getRemark = method(enumClass, "getRemark");

        return Stream.of(enumClass.getEnumConstants()).flatMap(constant -> {
            String name = enumClass.getSimpleName() + SymbolConstant.DOT + constant.name();
            return Stream.of(
                    DynamicTest.dynamicTest(name + " has non-blank code", () -> {
                        String code = (String) getCode.invoke(constant);
                        assertThat(code).as(name + " code").isNotBlank();
                    }),
                    DynamicTest.dynamicTest(name + " uses lowercase code", () -> {
                        String code = (String) getCode.invoke(constant);
                        assertThat(code).as(name + " code").isEqualTo(code.toLowerCase(Locale.ROOT));
                    }),
                    DynamicTest.dynamicTest(name + " has human-readable remark", () -> {
                        String code = (String) getCode.invoke(constant);
                        String remark = (String) getRemark.invoke(constant);
                        assertThat(remark).as(name + " remark").isNotBlank();
                        assertThat(remark).as(name + " remark").isNotEqualTo(code);
                    }));
        });
    }
}
