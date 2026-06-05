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
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeUtilTest {

    @Test
    void milliSecondsExposesEpoch() {
        Date date = new Date(1_700_000_000_000L);
        assertThat(TimeUtil.milliSeconds(date)).isEqualTo(1_700_000_000_000L);
    }

    @Test
    void localDateTimeBuildsDateFromEpoch() {
        Date date = TimeUtil.localDateTime(1_700_000_000_000L);
        assertThat(date.getTime()).isEqualTo(1_700_000_000_000L);
    }

    @Test
    void expireTimeShiftsByCalendarField() {
        Date now = new Date();
        Date plusOneHour = TimeUtil.expireTime(1, Calendar.HOUR);
        assertThat(plusOneHour.getTime()).isGreaterThan(now.getTime());
        assertThat(plusOneHour.getTime() - now.getTime())
                .isBetween(60L * 60_000L - 5_000L, 60L * 60_000L + 5_000L);
    }

    @Test
    void defaultFormatReversesDefaultDate() {
        Date original = new Date(1_700_000_000_000L);
        String formatted = TimeUtil.defaultFormat(original);
        Date parsed = TimeUtil.defaultDate(formatted);
        assertThat(formatted).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        assertThat(parsed).isNotNull();
    }

    @Test
    void completeFormatReversesCompleteDate() {
        Date original = new Date(1_700_000_000_456L);
        String formatted = TimeUtil.completeFormat(original);
        Date parsed = TimeUtil.completeDate(formatted);
        assertThat(formatted).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void defaultDateReturnsNullForUnparsableInput() {
        assertThat(TimeUtil.defaultDate("not-a-date")).isNull();
    }

    @Test
    void completeDateReturnsNullForUnparsableInput() {
        assertThat(TimeUtil.completeDate("garbage")).isNull();
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<TimeUtil> constructor = TimeUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
