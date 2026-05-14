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

package io.github.pnoker.common.agentic.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticTokenEstimatorTest {

    @Test
    void blankReturnsZero() {
        assertThat(AgenticTokenEstimator.estimate(null)).isZero();
        assertThat(AgenticTokenEstimator.estimate("")).isZero();
        assertThat(AgenticTokenEstimator.estimate("   ")).isZero();
    }

    @Test
    void singleCharacterAlwaysCostsAtLeastOneToken() {
        assertThat(AgenticTokenEstimator.estimate("a")).isOne();
        assertThat(AgenticTokenEstimator.estimate("中")).isOne();
    }

    @Test
    void asciiHeavyTextUsesFourBytePerTokenRatio() {
        // 16 ASCII chars -> ceil(16 / 4.0) = 4
        assertThat(AgenticTokenEstimator.estimate("abcdefghijklmnop")).isEqualTo(4);
    }

    @Test
    void cjkHeavyTextUsesTighterRatio() {
        // 4 CJK chars -> ceil(4 / 1.8) = 3
        assertThat(AgenticTokenEstimator.estimate("你好世界")).isEqualTo(3);
    }

    @Test
    void mixedAsciiAndNonAsciiSumIndependentRatios() {
        // 8 ASCII (2.0) + 2 CJK (1.111...) -> ceil(3.111...) = 4
        assertThat(AgenticTokenEstimator.estimate("hello you 你好")).isGreaterThanOrEqualTo(4);
    }

    @Test
    void utilityConstructorMustReject() throws NoSuchMethodException {
        Constructor<AgenticTokenEstimator> ctor = AgenticTokenEstimator.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThatThrownBy(ctor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
