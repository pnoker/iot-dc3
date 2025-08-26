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

import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.LambdaMeta;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.property.PropertyNamer;

import java.util.Objects;

/**
 * 字段名称 相关工具类
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
public class FieldUtil {

    private FieldUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 获取字段映射
     *
     * @param column 需要解析的 lambda 对象
     * @param <T>    对象的类型
     * @return Field Name
     */
    public static <T> String getField(SFunction<T, ?> column) {
        LambdaMeta meta = LambdaUtils.extract(column);
        return PropertyNamer.methodToProperty(meta.getImplMethodName());
    }

    /**
     * 判断是否为有效的ID
     *
     * @param id ID
     * @return 是否有效
     */
    public static boolean isValidIdField(Long id) {
        return Objects.nonNull(id) && id > DefaultConstant.ZERO;
    }

    /**
     * 判断是否为有效的枚举索引
     *
     * @param id ID
     * @return 是否有效
     */
    public static boolean isValidEnumIndexField(Byte id) {
        return Objects.nonNull(id) && id > DefaultConstant.ZERO;
    }
}
