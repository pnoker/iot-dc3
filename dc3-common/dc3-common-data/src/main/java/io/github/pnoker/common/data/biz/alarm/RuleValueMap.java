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

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ordered runtime value map helper used at the rule-engine boundary.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
final class RuleValueMap {

    private RuleValueMap() {
    }

    static Map<String, Object> from(Record snapshot) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (RecordComponent component : snapshot.getClass().getRecordComponents()) {
            values.put(component.getName(), read(snapshot, component));
        }
        return values;
    }

    private static Object read(Record snapshot, RecordComponent component) {
        try {
            component.getAccessor().setAccessible(true);
            return component.getAccessor().invoke(snapshot);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read rule snapshot field: " + component.getName(), e);
        }
    }

}
