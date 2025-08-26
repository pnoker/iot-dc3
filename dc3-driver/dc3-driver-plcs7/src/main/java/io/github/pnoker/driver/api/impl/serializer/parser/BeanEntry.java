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
package io.github.pnoker.driver.api.impl.serializer.parser;

import io.github.pnoker.driver.api.S7Serializable;
import io.github.pnoker.driver.api.S7Type;

import java.lang.reflect.Field;

/**
 * A Bean-Entry
 *
 * @author Thomas Rudin
 */
public final class BeanEntry {
    /**
     * The Array size
     */
    public int arraySize;

    /**
     * Offsets and size
     */
    public int byteOffset, bitOffset, size;

    /**
     * The corresponding field
     */
    public Field field;

    /**
     * Array type
     */
    public boolean isArray;

    /**
     * The S7 Type
     */
    public S7Type s7type;

    /**
     * The corresponding serializer
     */
    public S7Serializable serializer;

    /**
     * The Java type
     */
    public Class<?> type;
}
