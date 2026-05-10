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

import io.github.pnoker.common.constant.common.ExceptionConstant;
import org.apache.commons.lang3.StringUtils;

/**
 * Lightweight token estimator for audit records.
 * <p>
 * Provider-specific tokenizers are not available for every OpenAI-compatible model, so
 * this estimator is intentionally approximate.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
public class AgenticTokenEstimator {

    private AgenticTokenEstimator() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    public static int estimate(String text) {
        if (StringUtils.isBlank(text)) {
            return 0;
        }
        int ascii = 0;
        int nonAscii = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) <= 127) {
                ascii++;
            } else {
                nonAscii++;
            }
        }
        return Math.max(1, (int) Math.ceil(ascii / 4.0 + nonAscii / 1.8));
    }

}
