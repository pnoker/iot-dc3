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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for logging method execution in DC3 IoT Platform. This annotation can
 * be used to automatically generate logs for method calls, with configurable log type,
 * message, tags and persistence options.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Documented
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Logs {

    /**
     * The log message to be recorded
     *
     * @return The log message string
     */
    String value() default "";

    /**
     * The type/level of the log entry
     *
     * @return The LogsTypeEnum enum value
     */
    LogsTypeEnum type() default LogsTypeEnum.INFO;

    /**
     * Custom tag for categorizing or filtering logs
     *
     * @return The tag string
     */
    String tag() default "";

    /**
     * Whether to persist the log entry to storage
     *
     * @return True if the log should be saved, false otherwise
     */
    boolean save() default false;

}
