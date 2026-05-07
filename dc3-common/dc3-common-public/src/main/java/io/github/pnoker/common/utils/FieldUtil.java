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
 * Field Name Utility Class
 * <p>
 * Utility class for field name operations and lambda expressions. Provides methods to
 * extract field names from lambda functions and convert property names for database
 * operations.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
public class FieldUtil {

	private FieldUtil() {
		throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
	}

	/**
	 * Get the field name mapping from a lambda expression.
	 * @param column Lambda object to be parsed
	 * @param <T> Object type
	 * @return Field name
	 */
	public static <T> String getField(SFunction<T, ?> column) {
		LambdaMeta meta = LambdaUtils.extract(column);
		return PropertyNamer.methodToProperty(meta.getImplMethodName());
	}

	/**
	 * Determine whether the given ID field is valid.
	 * @param id ID
	 * @return Whether the ID is valid
	 */
	public static boolean isValidIdField(Long id) {
		return Objects.nonNull(id) && id > DefaultConstant.ZERO;
	}

	/**
	 * Determine whether the given enum index field is valid.
	 * @param id ID
	 * @return Whether the enum index is valid
	 */
	public static boolean isValidEnumIndexField(Byte id) {
		return Objects.nonNull(id) && id > DefaultConstant.ZERO;
	}

}
