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

package io.github.pnoker.common.utils;

import cn.hutool.core.util.IdUtil;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;

/**
 * 编码生成工具类, 提供唯一编码生成功能
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
public class CodeUtil {

    private CodeUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }


    /**
     * 生成一个简化的UUID作为唯一编码
     *
     * @return 返回一个简化的UUID字符串
     */
    public static String getCode() {
        return IdUtil.fastSimpleUUID();
    }
}
