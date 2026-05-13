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

import io.github.pnoker.common.enums.TimeRangeKeyEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TimeRangeUtilTest {

    @Test
    void resolveFromTodayReturnsStartOfDay() {
        LocalDateTime resolved = TimeRangeUtil.resolveFrom(TimeRangeKeyEnum.TODAY, null);
        assertThat(resolved).isEqualTo(LocalDate.now().atStartOfDay());
    }

    @Test
    void resolveFromH24ReturnsApproximatelyTwentyFourHoursAgo() {
        LocalDateTime resolved = TimeRangeUtil.resolveFrom(TimeRangeKeyEnum.H24, null);
        long deltaHours = ChronoUnit.HOURS.between(resolved, LocalDateTime.now());
        assertThat(deltaHours).isBetween(23L, 25L);
    }

    @Test
    void resolveFromD7ReturnsApproximatelySevenDaysAgo() {
        LocalDateTime resolved = TimeRangeUtil.resolveFrom(TimeRangeKeyEnum.D7, null);
        long deltaHours = ChronoUnit.HOURS.between(resolved, LocalDateTime.now());
        assertThat(deltaHours).isBetween(167L, 169L);
    }

    @Test
    void resolveFromD30ReturnsApproximatelyThirtyDaysAgo() {
        LocalDateTime resolved = TimeRangeUtil.resolveFrom(TimeRangeKeyEnum.D30, null);
        long deltaHours = ChronoUnit.HOURS.between(resolved, LocalDateTime.now());
        assertThat(deltaHours).isBetween(719L, 721L);
    }

    @Test
    void resolveFromFallsBackToHoursWhenKeyMissing() {
        LocalDateTime resolved = TimeRangeUtil.resolveFrom((TimeRangeKeyEnum) null, 5);
        long deltaHours = ChronoUnit.HOURS.between(resolved, LocalDateTime.now());
        assertThat(deltaHours).isBetween(4L, 6L);
    }

    @Test
    void resolveFromReturnsNullWhenNeitherKeyNorHoursProvided() {
        assertThat(TimeRangeUtil.resolveFrom((TimeRangeKeyEnum) null, null)).isNull();
        assertThat(TimeRangeUtil.resolveFrom((TimeRangeKeyEnum) null, 0)).isNull();
        assertThat(TimeRangeUtil.resolveFrom((TimeRangeKeyEnum) null, -1)).isNull();
    }

    @Test
    void resolveFromCodeOverloadDelegatesToEnumLookup() {
        LocalDateTime byCode = TimeRangeUtil.resolveFrom("24h", null);
        LocalDateTime byEnum = TimeRangeUtil.resolveFrom(TimeRangeKeyEnum.H24, null);
        assertThat(ChronoUnit.MINUTES.between(byCode, byEnum)).isLessThanOrEqualTo(1L);
    }

    @Test
    void resolveHoursMapsKeysToFixedSpans() {
        assertThat(TimeRangeUtil.resolveHours(TimeRangeKeyEnum.H24, null)).isEqualTo(24);
        assertThat(TimeRangeUtil.resolveHours(TimeRangeKeyEnum.D7, null)).isEqualTo(168);
        assertThat(TimeRangeUtil.resolveHours(TimeRangeKeyEnum.D30, null)).isEqualTo(720);
    }

    @Test
    void resolveHoursForTodayReturnsAtLeastOne() {
        Integer hours = TimeRangeUtil.resolveHours(TimeRangeKeyEnum.TODAY, null);
        assertThat(hours).isGreaterThanOrEqualTo(1);
    }

    @Test
    void resolveHoursReturnsCustomValueWhenKeyMissing() {
        assertThat(TimeRangeUtil.resolveHours((TimeRangeKeyEnum) null, 9)).isEqualTo(9);
    }

    @Test
    void resolveHoursReturnsNullForBlankInputs() {
        assertThat(TimeRangeUtil.resolveHours((TimeRangeKeyEnum) null, null)).isNull();
        assertThat(TimeRangeUtil.resolveHours((TimeRangeKeyEnum) null, 0)).isNull();
    }

    @Test
    void resolveDaysMapsKeysToFixedDays() {
        assertThat(TimeRangeUtil.resolveDays(TimeRangeKeyEnum.TODAY, null)).isEqualTo(1);
        assertThat(TimeRangeUtil.resolveDays(TimeRangeKeyEnum.H24, null)).isEqualTo(1);
        assertThat(TimeRangeUtil.resolveDays(TimeRangeKeyEnum.D7, null)).isEqualTo(7);
        assertThat(TimeRangeUtil.resolveDays(TimeRangeKeyEnum.D30, null)).isEqualTo(30);
    }

    @Test
    void resolveDaysReturnsCustomValueWhenKeyMissing() {
        assertThat(TimeRangeUtil.resolveDays((TimeRangeKeyEnum) null, 14)).isEqualTo(14);
    }

    @Test
    void resolveDaysReturnsNullForBlankInputs() {
        assertThat(TimeRangeUtil.resolveDays((TimeRangeKeyEnum) null, null)).isNull();
    }

    @Test
    void resolveDaysCodeOverloadDelegates() {
        assertThat(TimeRangeUtil.resolveDays("7d", null)).isEqualTo(7);
    }
}
