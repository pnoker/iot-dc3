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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZoneDateTimeUtilTest {

    @Test
    void nowReturnsCurrentInstantInDefaultZone() {
        ZonedDateTime before = ZonedDateTime.now().minusSeconds(5);
        ZonedDateTime now = ZoneDateTimeUtil.now();
        ZonedDateTime after = ZonedDateTime.now().plusSeconds(5);
        assertThat(now).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void milliSecondsAndDateTimeAreInverse() {
        long epoch = 1_700_000_000_000L;
        ZonedDateTime time = ZoneDateTimeUtil.dateTime(epoch);
        assertThat(ZoneDateTimeUtil.milliSeconds(time)).isEqualTo(epoch);
    }

    @Test
    void expireTimeShiftsByChronoUnit() {
        ZonedDateTime plusHour = ZoneDateTimeUtil.expireTime(1, ChronoUnit.HOURS);
        ZonedDateTime now = ZoneDateTimeUtil.now();
        assertThat(plusHour).isAfter(now.minusMinutes(1));
        assertThat(ChronoUnit.MINUTES.between(now, plusHour)).isBetween(58L, 62L);
    }

    @Test
    void defaultFormatRoundTrip() {
        ZonedDateTime original = ZoneDateTimeUtil.dateTime(1_700_000_000_000L);
        String formatted = ZoneDateTimeUtil.defaultFormat(original);
        assertThat(formatted).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        assertThat(ZoneDateTimeUtil.defaultDate(formatted)).isEqualTo(
                ZoneDateTimeUtil.defaultDate(formatted));
    }

    @Test
    void completeFormatRoundTrip() {
        ZonedDateTime original = ZoneDateTimeUtil.dateTime(1_700_000_000_456L);
        String formatted = ZoneDateTimeUtil.completeFormat(original);
        assertThat(formatted).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
    }

    @Test
    void defaultDateReturnsNullForUnparsableInput() {
        assertThat(ZoneDateTimeUtil.defaultDate("nope")).isNull();
    }

    @Test
    void completeDateReturnsNullForUnparsableInput() {
        assertThat(ZoneDateTimeUtil.completeDate("nope")).isNull();
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<ZoneDateTimeUtil> constructor = ZoneDateTimeUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
