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

import cn.hutool.core.text.CharSequenceUtil;

/**
 * 自定义 导入 异常
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class ImportException extends RuntimeException {
    public ImportException() {
        this(null);
    }

    public ImportException(Throwable cause) {
        super(cause);
    }

    public ImportException(CharSequence template, Object... params) {
        super(CharSequenceUtil.format(template, params));
    }
}
