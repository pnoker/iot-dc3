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

import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

/**
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
public class ExceptionUtil {

	private ExceptionUtil() {
		throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
	}

	/**
	 * @param service Name
	 * @param message
	 * @return
	 */
	public static String getNotAvailableServiceMessage(String service, String message) {
		if (StringUtils.isEmpty(message)) {
			message = MessageFormat.format("{0}: {1}", ExceptionConstant.NO_AVAILABLE_SERVER, service);
		}
		return message;
	}

}
