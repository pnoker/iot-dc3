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
package io.github.pnoker.driver.api.annotation;

import io.github.pnoker.driver.api.S7Type;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for mapping a Java field to an S7 PLC data block variable. Specifies the S7
 * type, byte/bit offsets, and optional size for the mapping.
 *
 * @author Thomas Rudin
 */
@Target(value = {ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface S7Variable {

    /**
     * @return number of elements (for array fields)
     */
    int arraySize() default 1;

    /**
     * @return bit offset within the byte
     */
    int bitOffset() default 0;

    /**
     * @return byte offset within the data block
     */
    int byteOffset();

    /**
     * @return byte size (required for STRING type)
     */
    int size() default 0;

    /**
     * @return the S7 data type
     */
    S7Type type();

}
