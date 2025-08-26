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

package io.github.pnoker.common.constant.driver;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SuffixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;

/**
 * 存储 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class StorageConstant {

    /**
     * 设备数据存储集合前缀
     */
    public static final String POINT_VALUE_PREFIX = PrefixConstant.POINT + SuffixConstant.VALUE + SymbolConstant.UNDERSCORE;

    private StorageConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
