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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Normalized fact used by the deterministic alarm rule engine.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RuleFact {

    private Long tenantId;

    private AlarmTargetTypeFlagEnum alarmTargetTypeFlag;

    private Long entityId;

    private Long alarmId;

    private LocalDateTime factTime;

    private Map<String, Object> values;

    /**
     * Return a normalized fact field value.
     *
     * @param field fact field name
     * @return field value or {@code null}
     */
    public Object value(String field) {
        if (Objects.isNull(values) || Objects.isNull(field)) {
            return null;
        }
        return values.get(field);
    }

}
