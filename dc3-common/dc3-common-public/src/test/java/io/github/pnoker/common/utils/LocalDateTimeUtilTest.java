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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.common.TimeConstant;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDateTimeUtilTest {

    @Test
    void nowReturnsCurrentInstantInDefaultZone() {
        // LocalDateTimeUtil.now() uses TimeConstant.DEFAULT_ZONEID, so the bounds must use the
        // same zone — otherwise this fails on any machine whose JVM default zone differs (e.g. CI on UTC).
        LocalDateTime before = LocalDateTime.now(TimeConstant.DEFAULT_ZONEID).minusSeconds(5);
        LocalDateTime now = LocalDateTimeUtil.now();
        LocalDateTime after = LocalDateTime.now(TimeConstant.DEFAULT_ZONEID).plusSeconds(5);
        assertThat(now).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void milliSecondsAndDateTimeAreInverse() {
        long epoch = 1_700_000_000_000L;
        LocalDateTime time = LocalDateTimeUtil.dateTime(epoch);
        assertThat(LocalDateTimeUtil.milliSeconds(time)).isEqualTo(epoch);
    }

    @Test
    void expireTimeShiftsByChronoUnit() {
        LocalDateTime now = LocalDateTimeUtil.now();
        LocalDateTime plusHour = LocalDateTimeUtil.expireTime(1, ChronoUnit.HOURS);
        assertThat(plusHour).isAfter(now);
        assertThat(ChronoUnit.HOURS.between(now, plusHour)).isBetween(0L, 1L);
    }

    @Test
    void defaultFormatRoundTripsParseAndFormat() {
        LocalDateTime original = LocalDateTime.of(2026, 1, 1, 12, 30, 45);
        String formatted = LocalDateTimeUtil.defaultFormat(original);
        assertThat(formatted).isEqualTo("2026-01-01 12:30:45");
        assertThat(LocalDateTimeUtil.defaultDate(formatted)).isEqualTo(original);
    }

    @Test
    void completeFormatRoundTripsParseAndFormat() {
        LocalDateTime original = LocalDateTime.of(2026, 1, 1, 12, 30, 45, 123_000_000);
        String formatted = LocalDateTimeUtil.completeFormat(original);
        assertThat(formatted).isEqualTo("2026-01-01 12:30:45.123");
        assertThat(LocalDateTimeUtil.completeDate(formatted)).isEqualTo(original);
    }

    @Test
    void defaultDateReturnsNullForUnparsableInput() {
        assertThat(LocalDateTimeUtil.defaultDate("nope")).isNull();
    }

    @Test
    void completeDateReturnsNullForUnparsableInput() {
        assertThat(LocalDateTimeUtil.completeDate("nope")).isNull();
    }

    @Test
    void formatterFactoriesAreNotNull() {
        assertThat(LocalDateTimeUtil.getDefaultDateTimeFormatter()).isNotNull();
        assertThat(LocalDateTimeUtil.getCompleteDateTimeFormatter()).isNotNull();
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<LocalDateTimeUtil> constructor = LocalDateTimeUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
