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

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Renders structured templates with named variables.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
public class AlarmTemplateRenderer {

    /**
     * Render a structured map while preserving unresolved placeholders.
     *
     * @param template  template map
     * @param variables variables
     * @return rendered map
     */
    public Map<String, Object> renderMap(Map<String, Object> template, Map<String, Object> variables) {
        if (Objects.isNull(template)) {
            return Map.of();
        }
        Map<String, Object> rendered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : template.entrySet()) {
            rendered.put(entry.getKey(), renderValue(entry.getValue(), variables));
        }
        return rendered;
    }

    /**
     * Render text while preserving unresolved placeholders.
     *
     * @param text      template text
     * @param variables variables
     * @return rendered text
     */
    public String renderText(String text, Map<String, Object> variables) {
        Map<String, Object> safeVariables = Objects.requireNonNullElse(variables, Map.of());
        StringBuilder rendered = new StringBuilder();
        int cursor = 0;
        while (cursor < text.length()) {
            int start = text.indexOf("${", cursor);
            if (start < 0) {
                rendered.append(text.substring(cursor));
                break;
            }
            int end = text.indexOf('}', start + 2);
            if (end < 0) {
                rendered.append(text.substring(cursor));
                break;
            }
            rendered.append(text, cursor, start);
            String variable = text.substring(start + 2, end);
            Object value = safeVariables.get(variable);
            rendered.append(Objects.nonNull(value) ? value : text.substring(start, end + 1));
            cursor = end + 1;
        }
        return rendered.toString();
    }

    private Object renderValue(Object value, Map<String, Object> variables) {
        if (value instanceof String text) {
            return renderText(text, variables);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> rendered = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                rendered.put(Objects.toString(entry.getKey(), ""), renderValue(entry.getValue(), variables));
            }
            return rendered;
        }
        if (value instanceof List<?> list) {
            List<Object> rendered = new ArrayList<>();
            for (Object item : list) {
                rendered.add(renderValue(item, variables));
            }
            return rendered;
        }
        return value;
    }

}
