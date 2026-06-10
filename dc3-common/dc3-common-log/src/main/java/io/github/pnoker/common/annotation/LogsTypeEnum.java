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

package io.github.pnoker.common.annotation;

import lombok.Getter;

/**
 * Enumeration of log levels used by the DC3 IoT Platform.
 *
 * <ul>
 *   <li>{@link #INFO}: general information messages</li>
 *   <li>{@link #WARN}: warning messages indicating potential issues</li>
 *   <li>{@link #DEBUG}: detailed information for development and troubleshooting</li>
 *   <li>{@link #ERROR}: error messages for failures</li>
 * </ul>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
public enum LogsTypeEnum {

    INFO, WARN, DEBUG, ERROR

}
