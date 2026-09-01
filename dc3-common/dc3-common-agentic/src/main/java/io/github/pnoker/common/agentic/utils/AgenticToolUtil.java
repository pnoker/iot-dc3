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
package io.github.pnoker.common.agentic.utils;

import io.github.pnoker.common.constant.common.BaseConstant;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared helpers for platform-bound agentic tools.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public class AgenticToolUtil {

    private AgenticToolUtil() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    /**
     * Normalize identifiers.
     *
     * @param ids ids
     * @return normalize identifiers result
     */
    public static List<Long> normalizeIds(List<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .limit(AgenticConstant.ToolLimit.MAX_IDS)
                .toList();
    }

    /**
     * Clamp.
     *
     * @param value value
     * @param min   min
     * @param max   max
     * @return clamp result
     */
    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * Determine whether empty.
     *
     * @param values values
     * @return {@code true} when the collection is {@code null} or empty
     */
    public static boolean isEmpty(Collection<?> values) {
        return Objects.isNull(values) || values.isEmpty();
    }

    /**
     * Determine whether empty.
     *
     * @param values values
     * @return {@code true} when the map is {@code null} or empty
     */
    public static boolean isEmpty(Map<?, ?> values) {
        return Objects.isNull(values) || values.isEmpty();
    }

    /** Determine whether an offset page contains at least one item. */
    public static boolean hasItems(OffsetPage<?> page) {
        return Objects.nonNull(page) && !isEmpty(page.items());
    }

}
