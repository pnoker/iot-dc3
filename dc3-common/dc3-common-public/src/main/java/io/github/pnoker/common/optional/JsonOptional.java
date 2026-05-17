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

package io.github.pnoker.common.optional;

import io.github.pnoker.common.utils.JsonUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

/**
 * Optional wrapper that checks for non-empty valid JSON strings.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public final class JsonOptional {

    private final String value;

    private JsonOptional(String value) {
        this.value = value;
    }

    public static JsonOptional ofNullable(String value) {
        return new JsonOptional(value);
    }

    public void ifPresent(Consumer<String> action) {
        if (StringUtils.isNotEmpty(value) && JsonUtil.isJson(value)) {
            action.accept(value);
        }
    }

    public void ifPresentOrElse(Consumer<String> action, Runnable emptyAction) {
        if (StringUtils.isNotEmpty(value) && JsonUtil.isJson(value)) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

}
