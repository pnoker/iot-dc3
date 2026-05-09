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

package io.github.pnoker.common.exception;

/**
 * Lightweight exception message formatter that supports the placeholder style used
 * by project exception call sites.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.9
 */
final class ExceptionMessageFormatter {

    static String format(String template, Object... params) {
        if (template == null || params == null || params.length == 0) {
            return template;
        }

        StringBuilder builder = new StringBuilder(template.length() + params.length * 16);
        int searchIndex = 0;
        int paramIndex = 0;
        while (paramIndex < params.length) {
            int placeholderIndex = template.indexOf("{}", searchIndex);
            if (placeholderIndex < 0) {
                break;
            }
            builder.append(template, searchIndex, placeholderIndex);
            builder.append(String.valueOf(params[paramIndex++]));
            searchIndex = placeholderIndex + 2;
        }
        builder.append(template, searchIndex, template.length());
        return builder.toString();
    }

    private ExceptionMessageFormatter() {
    }

}
