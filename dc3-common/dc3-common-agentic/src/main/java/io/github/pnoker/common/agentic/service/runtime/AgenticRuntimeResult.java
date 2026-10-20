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
package io.github.pnoker.common.agentic.service.runtime;

import io.github.pnoker.common.constant.service.AgenticConstant;
import java.util.Locale;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Final model result produced by the agentic runtime.
 *
 * @param content     final assistant content
 * @param finishReason provider finish reason; {@code max_rounds} marks a turn closed by
 *                     the loop budget with a forced no-tools final round
 * @param usage       provider-reported token usage for the turn, empty when unreported
 * @author pnoker
 * @since 2016.10.1
 */
public record AgenticRuntimeResult(String content, String finishReason, AgenticTokenUsage usage) {

    public static final String FINISH_REASON_MAX_ROUNDS = "max_rounds";

    /** Agentic runtime result compact constructor: normalizes the record. */
    public AgenticRuntimeResult {
        content = Objects.toString(content, "");
        finishReason = StringUtils.isNotBlank(finishReason)
                ? finishReason.trim().toLowerCase(Locale.ROOT)
                : AgenticConstant.Chat.FINISH_REASON_STOP;
        usage = Objects.requireNonNullElseGet(usage, AgenticTokenUsage::empty);
    }

    /**
     * Legacy two-argument constructor for callers without usage data.
     *
     * @param content     final assistant content
     * @param finishReason provider finish reason
     */
    public AgenticRuntimeResult(String content, String finishReason) {
        this(content, finishReason, AgenticTokenUsage.empty());
    }
}
