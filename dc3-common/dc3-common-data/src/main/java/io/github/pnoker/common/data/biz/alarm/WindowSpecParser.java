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

import io.github.pnoker.common.constant.common.BaseConstant;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.enums.WindowMode;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Parses {@link RuleExt.Window} JSON into a {@link WindowSpec}. Both the rule
 * service (save-time validation) and the runtime evaluator route through this
 * parser so the validation rules cannot drift between the two paths.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
public final class WindowSpecParser {

    private WindowSpecParser() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    /**
     * Resolves a {@link WindowSpec} from the raw extension. Treats null window
     * (or null/blank mode) as LAST so legacy rules without a window block keep
     * working untouched.
     */
    public static WindowSpec parse(RuleExt.Window window) {
        if (Objects.isNull(window) || StringUtils.isBlank(window.getMode())) {
            return WindowSpec.last();
        }
        WindowMode mode = WindowMode.ofCode(window.getMode());
        if (Objects.isNull(mode)) {
            return WindowSpec.invalid("Unknown window mode '" + window.getMode() + "'");
        }
        if (!mode.requiresDuration() && StringUtils.isBlank(window.getDuration())) {
            return WindowSpec.ok(mode, null, normalizedMinSamples(window, mode));
        }
        Duration duration;
        try {
            duration = Duration.parse(StringUtils.defaultIfBlank(window.getDuration(), "PT0S"));
        } catch (DateTimeParseException e) {
            return WindowSpec.invalid("Invalid window duration '" + window.getDuration()
                    + "', expected ISO-8601 like PT5M");
        }
        if (mode.requiresDuration() && (duration.isZero() || duration.isNegative())) {
            return WindowSpec.invalid("Window duration must be positive for mode " + mode);
        }
        return WindowSpec.ok(mode, mode == WindowMode.LAST ? null : duration,
                normalizedMinSamples(window, mode));
    }

    private static int normalizedMinSamples(RuleExt.Window window, WindowMode mode) {
        Integer raw = window.getMinSamples();
        if (Objects.isNull(raw) || raw < 0) {
            // Default: LAST always needs the current sample (=1); aggregating
            // modes default to 1 too — operators that want a stricter floor
            // should set minSamples explicitly.
            return 1;
        }
        if (mode == WindowMode.LAST && raw < 1) {
            return 1;
        }
        return raw;
    }

}
