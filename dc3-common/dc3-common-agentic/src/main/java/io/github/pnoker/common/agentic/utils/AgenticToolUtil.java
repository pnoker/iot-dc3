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
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.facade.entity.common.FacadePage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared helpers for platform-bound agentic tools.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
public class AgenticToolUtil {

    private AgenticToolUtil() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    public static List<Long> normalizeIds(List<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(AgenticConstant.ToolLimit.MAX_IDS)
                .toList();
    }

    public static Pages page(int current, int size) {
        Pages page = new Pages();
        page.setCurrent(current);
        page.setSize(size);
        return page;
    }

    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static boolean isEmpty(Collection<?> values) {
        return Objects.isNull(values) || values.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> values) {
        return Objects.isNull(values) || values.isEmpty();
    }

    public static boolean hasRecords(FacadePage<?> page) {
        return Objects.nonNull(page) && !isEmpty(page.getRecords());
    }

}
