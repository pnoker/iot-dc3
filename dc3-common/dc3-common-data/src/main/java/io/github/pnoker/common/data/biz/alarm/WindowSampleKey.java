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

import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;

/**
 * Identity of a window sample buffer. We key on {@code (tenantId, targetType,
 * entityId)} rather than per rule because a single entity can have multiple
 * rules with different durations — sharing one buffer per entity lets all of
 * those rules read from the same time-ordered series.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
public record WindowSampleKey(Long tenantId, AlarmTargetTypeFlagEnum targetType, Long entityId) {

    public static WindowSampleKey of(Long tenantId, AlarmTargetTypeFlagEnum targetType, Long entityId) {
        return new WindowSampleKey(tenantId, targetType, entityId);
    }

}
