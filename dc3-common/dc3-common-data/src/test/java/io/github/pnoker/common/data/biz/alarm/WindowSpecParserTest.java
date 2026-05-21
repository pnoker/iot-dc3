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

import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.enums.WindowMode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class WindowSpecParserTest {

    @Test
    void nullWindowParsesAsLast() {
        WindowSpec spec = WindowSpecParser.parse(null);
        assertThat(spec.valid()).isTrue();
        assertThat(spec.mode()).isEqualTo(WindowMode.LAST);
        assertThat(spec.duration()).isNull();
    }

    @Test
    void blankModeFallsBackToLast() {
        WindowSpec spec = WindowSpecParser.parse(new RuleExt.Window("  ", "PT5M", 1));
        assertThat(spec.mode()).isEqualTo(WindowMode.LAST);
        assertThat(spec.duration()).isNull();
    }

    @Test
    void unknownModeProducesInvalidSpec() {
        WindowSpec spec = WindowSpecParser.parse(new RuleExt.Window("FOOBAR", "PT5M", 1));
        assertThat(spec.valid()).isFalse();
        assertThat(spec.reason()).contains("FOOBAR");
    }

    @Test
    void avgWithValidDurationParsesCorrectly() {
        WindowSpec spec = WindowSpecParser.parse(new RuleExt.Window("AVG", "PT5M", 3));
        assertThat(spec.valid()).isTrue();
        assertThat(spec.mode()).isEqualTo(WindowMode.AVG);
        assertThat(spec.duration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(spec.minSamples()).isEqualTo(3);
    }

    @Test
    void caseInsensitiveModeAccepted() {
        WindowSpec spec = WindowSpecParser.parse(new RuleExt.Window("avg", "PT5M", 1));
        assertThat(spec.mode()).isEqualTo(WindowMode.AVG);
    }

    @Test
    void aggregationModeRejectsNonPositiveDuration() {
        WindowSpec zero = WindowSpecParser.parse(new RuleExt.Window("AVG", "PT0S", 1));
        WindowSpec negative = WindowSpecParser.parse(new RuleExt.Window("AVG", "-PT5M", 1));
        assertThat(zero.valid()).isFalse();
        assertThat(zero.reason()).contains("positive");
        assertThat(negative.valid()).isFalse();
    }

    @Test
    void aggregationModeRejectsMalformedDuration() {
        WindowSpec spec = WindowSpecParser.parse(new RuleExt.Window("AVG", "5 minutes", 1));
        assertThat(spec.valid()).isFalse();
        assertThat(spec.reason()).contains("ISO-8601");
    }

    @Test
    void lastTreatsZeroMinSamplesAsOne() {
        WindowSpec spec = WindowSpecParser.parse(new RuleExt.Window("LAST", null, 0));
        assertThat(spec.mode()).isEqualTo(WindowMode.LAST);
        assertThat(spec.minSamples()).isEqualTo(1);
    }

    @Test
    void aggregationModeAcceptsZeroMinSamples() {
        WindowSpec spec = WindowSpecParser.parse(new RuleExt.Window("COUNT", "PT5M", 0));
        assertThat(spec.minSamples()).isEqualTo(0);
    }

    @Test
    void allAndAnyModesRequireDuration() {
        // ALL/ANY are sample-folding modes, so they need a window to bound
        // the fold. A blank duration is a parse error, not a fallback.
        WindowSpec all = WindowSpecParser.parse(new RuleExt.Window("ALL", "", 1));
        assertThat(all.valid()).isFalse();
    }

}
