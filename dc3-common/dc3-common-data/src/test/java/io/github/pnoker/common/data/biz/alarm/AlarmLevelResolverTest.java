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

import io.github.pnoker.common.enums.AlarmMessageLevelEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlarmLevelResolverTest {

    @Test
    void resolvesUpperCaseSeverityName() {
        assertThat(AlarmLevelResolver.resolve("P0", AlarmMessageLevelEnum.P2)).isEqualTo(AlarmMessageLevelEnum.P0);
        assertThat(AlarmLevelResolver.resolve("P1", AlarmMessageLevelEnum.P2)).isEqualTo(AlarmMessageLevelEnum.P1);
        assertThat(AlarmLevelResolver.resolve("P2", AlarmMessageLevelEnum.P2)).isEqualTo(AlarmMessageLevelEnum.P2);
        assertThat(AlarmLevelResolver.resolve("P3", AlarmMessageLevelEnum.P2)).isEqualTo(AlarmMessageLevelEnum.P3);
    }

    @Test
    void resolvesLowerCaseSeverityCode() {
        assertThat(AlarmLevelResolver.resolve("p0", AlarmMessageLevelEnum.P2)).isEqualTo(AlarmMessageLevelEnum.P0);
        assertThat(AlarmLevelResolver.resolve("p3", AlarmMessageLevelEnum.P2)).isEqualTo(AlarmMessageLevelEnum.P3);
    }

    @Test
    void normalizesWhitespaceAndMixedCase() {
        assertThat(AlarmLevelResolver.resolve("  p1  ", AlarmMessageLevelEnum.P2)).isEqualTo(AlarmMessageLevelEnum.P1);
        assertThat(AlarmLevelResolver.resolve("Critical", AlarmMessageLevelEnum.P2))
                .as("unrecognized labels fall back to the supplied default")
                .isEqualTo(AlarmMessageLevelEnum.P2);
    }

    @Test
    void usesFallbackWhenSeverityIsBlankOrNull() {
        assertThat(AlarmLevelResolver.resolve(null, AlarmMessageLevelEnum.P2))
                .isEqualTo(AlarmMessageLevelEnum.P2);
        assertThat(AlarmLevelResolver.resolve("", AlarmMessageLevelEnum.P3))
                .isEqualTo(AlarmMessageLevelEnum.P3);
        assertThat(AlarmLevelResolver.resolve("   ", AlarmMessageLevelEnum.P0))
                .isEqualTo(AlarmMessageLevelEnum.P0);
    }

}
