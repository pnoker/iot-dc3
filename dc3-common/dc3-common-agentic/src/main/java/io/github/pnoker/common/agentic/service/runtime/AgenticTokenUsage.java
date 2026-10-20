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

/**
 * Token usage reported by the provider for one chat turn. Values are {@code null} when
 * the provider did not report usage; consumers must fall back to estimation in that
 * case and mark the estimate as such.
 *
 * @param promptTokens     tokens consumed by the prompt (tools and tool results included)
 * @param completionTokens tokens produced by the completion
 * @param totalTokens      total tokens for the turn
 * @author pnoker
 * @since 2026.10.10
 */
public record AgenticTokenUsage(Long promptTokens, Long completionTokens, Long totalTokens) {

    /** Empty usage: the provider reported nothing. */
    public static AgenticTokenUsage empty() {
        return new AgenticTokenUsage(null, null, null);
    }

    /**
     * Determine whether any usage number is present.
     *
     * @return {@code true} when at least one component is reported
     */
    public boolean isPresent() {
        return promptTokens != null || completionTokens != null || totalTokens != null;
    }
}
