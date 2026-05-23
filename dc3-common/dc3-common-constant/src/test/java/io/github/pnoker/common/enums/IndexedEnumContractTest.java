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

import io.github.pnoker.common.constant.common.SymbolConstant;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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
 * <p>Adding a new enum that follows the convention only requires adding its class
 * literal to the {@link #INDEXED_ENUMS} list. {@link TimeRangeKeyEnum} is excluded
 * intentionally because it keys off a string code rather than an integer index;
 * its contract is exercised in the dc3-common-public TimeRangeUtilTest.
 */
class IndexedEnumContractTest {

    private static final List<Class<? extends Enum<?>>> INDEXED_ENUMS = List.of(
            AccrueFlagEnum.class,
            AgenticActionStatusEnum.class,
            AgenticMessageStatusEnum.class,
            AgenticModelProviderTypeEnum.class,
            AlarmMessageLevelFlagEnum.class,
            AlarmSourceFlagEnum.class,
            AlarmTargetTypeFlagEnum.class,
            AlarmTypeFlagEnum.class,
            ApiTypeFlagEnum.class,
            AttributeTypeFlagEnum.class,
            AutoConfirmFlagEnum.class,
            DefaultFlagEnum.class,
            PointCommandTypeEnum.class,
            EntityStatusEnum.class,
            DriverTypeFlagEnum.class,
            EnableFlagEnum.class,
            EntityTypeFlagEnum.class,
            ExpireFlagEnum.class,
            MenuLevelFlagEnum.class,
            MenuTypeFlagEnum.class,
            MetadataOperateTypeEnum.class,
            MetadataTypeEnum.class,
            NotifyChannelTypeFlagEnum.class,
            NotifyHistoryStatusEnum.class,
            PointTypeFlagEnum.class,
            ProfileShareFlagEnum.class,
            ProfileTypeFlagEnum.class,
            ResourceScopeFlagEnum.class,
            ResourceTypeFlagEnum.class,
            ResponseEnum.class,
            RuleStateFlagEnum.class,
            RwFlagEnum.class);

    private static Method method(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(
                    "Enum %s does not expose %s(%s)".formatted(
                            type.getName(), name, String.join(",",
                                    Stream.of(parameterTypes).map(Class::getSimpleName).toList())),
                    e);
        }
    }

    private static Byte unusedIndex(Enum<?>[] constants, Method getIndex) throws Exception {
        Set<Byte> taken = new HashSet<>();
        for (Enum<?> constant : constants) {
            taken.add((Byte) getIndex.invoke(constant));
        }
        // Search in the full Byte range so wide-coded enums like ResponseEnum
        // (codes around 200, 500, 20301) still find a free slot.
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
                                .as("%s index %s must be unique within %s", name, index,
                                        enumClass.getSimpleName())
                                .isTrue();
                    }),
                    DynamicTest.dynamicTest(name + " round-trips through ofIndex", () -> {
                        Byte index = (Byte) getIndex.invoke(constant);
                        Object resolved = ofIndex.invoke(null, index);
                        assertThat(resolved).isEqualTo(constant);
                    }));
        });

        Stream<DynamicTest> rejection = Stream.of(
                DynamicTest.dynamicTest(
                        enumClass.getSimpleName() + ".ofIndex returns null for unknown index",
                        () -> {
                            Byte unknown = unusedIndex(constants, getIndex);
                            assertThat(ofIndex.invoke(null, unknown)).isNull();
                        }));

        return Stream.concat(perConstant, rejection);
    }
}
