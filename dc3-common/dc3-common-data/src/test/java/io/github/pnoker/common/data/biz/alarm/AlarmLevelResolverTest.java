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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlarmLevelResolverTest {

    @Test
    void resolvesUpperCaseSeverityName() {
        assertThat(AlarmLevelResolver.resolve("P0", AlarmMessageLevelFlagEnum.P2)).isEqualTo(AlarmMessageLevelFlagEnum.P0);
        assertThat(AlarmLevelResolver.resolve("P1", AlarmMessageLevelFlagEnum.P2)).isEqualTo(AlarmMessageLevelFlagEnum.P1);
        assertThat(AlarmLevelResolver.resolve("P2", AlarmMessageLevelFlagEnum.P2)).isEqualTo(AlarmMessageLevelFlagEnum.P2);
        assertThat(AlarmLevelResolver.resolve("P3", AlarmMessageLevelFlagEnum.P2)).isEqualTo(AlarmMessageLevelFlagEnum.P3);
    }

    @Test
    void resolvesLowerCaseSeverityCode() {
        assertThat(AlarmLevelResolver.resolve("p0", AlarmMessageLevelFlagEnum.P2)).isEqualTo(AlarmMessageLevelFlagEnum.P0);
        assertThat(AlarmLevelResolver.resolve("p3", AlarmMessageLevelFlagEnum.P2)).isEqualTo(AlarmMessageLevelFlagEnum.P3);
    }

    @Test
    void normalizesWhitespaceAndMixedCase() {
        assertThat(AlarmLevelResolver.resolve("  p1  ", AlarmMessageLevelFlagEnum.P2)).isEqualTo(AlarmMessageLevelFlagEnum.P1);
        assertThat(AlarmLevelResolver.resolve("Critical", AlarmMessageLevelFlagEnum.P2))
                .as("unrecognized labels fall back to the supplied default")
                .isEqualTo(AlarmMessageLevelFlagEnum.P2);
    }

    @Test
    void usesFallbackWhenSeverityIsBlankOrNull() {
        assertThat(AlarmLevelResolver.resolve(null, AlarmMessageLevelFlagEnum.P2))
                .isEqualTo(AlarmMessageLevelFlagEnum.P2);
        assertThat(AlarmLevelResolver.resolve("", AlarmMessageLevelFlagEnum.P3))
                .isEqualTo(AlarmMessageLevelFlagEnum.P3);
        assertThat(AlarmLevelResolver.resolve("   ", AlarmMessageLevelFlagEnum.P0))
                .isEqualTo(AlarmMessageLevelFlagEnum.P0);
    }

}
