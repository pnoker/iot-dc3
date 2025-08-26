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

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.driver.api.S7Serializable;
import io.github.pnoker.driver.api.S7Type;
import io.github.pnoker.driver.api.annotation.S7Variable;
import io.github.pnoker.driver.bean.PlcS7PointVariable;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
public final class BeanParser {

    private BeanParser() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Returns the wrapper for the primitive type
     *
     * @param primitiveType Class
     * @return Class
     */
    private static Class<?> getWrapperForPrimitiveType(final Class<?> primitiveType) {
        if (primitiveType == boolean.class) {
            return Boolean.class;
        } else if (primitiveType == byte.class) {
            return Byte.class;
        } else if (primitiveType == int.class) {
            return Integer.class;
        } else if (primitiveType == float.class) {
            return Float.class;
        } else if (primitiveType == double.class) {
            return Double.class;
        } else if (primitiveType == long.class) {
            return Long.class;
        } else {
            // Fallback
            return primitiveType;
        }
    }


    public static BeanEntry parse(PlcS7PointVariable plcs7PointVariable) throws Exception {
        final BeanEntry entry = new BeanEntry();
        entry.byteOffset = plcs7PointVariable.getByteOffset();
        entry.bitOffset = plcs7PointVariable.getBitOffset();
        entry.size = plcs7PointVariable.getSize();
        entry.s7type = plcs7PointVariable.getType();
        entry.type = getWrapperForPrimitiveType(plcs7PointVariable.getFieldType());
        entry.serializer = entry.s7type.getSerializer().getDeclaredConstructor().newInstance();

        return entry;
    }

    /**
     * Parses a Class
     *
     * @param jclass Class
     * @return BeanParseResult
     * @throws Exception Exception
     */
    public static BeanParseResult parse(final Class<?> jclass) throws Exception {
        final BeanParseResult res = new BeanParseResult();
        log.trace("Parsing: " + jclass.getName());

        for (final Field field : jclass.getFields()) {
            final S7Variable dataAnnotation = field.getAnnotation(S7Variable.class);

            if (dataAnnotation != null) {
                log.trace("Parsing field: " + field.getName());
                log.trace("		type: " + dataAnnotation.type());
                log.trace("		byteOffset: " + dataAnnotation.byteOffset());
                log.trace("		bitOffset: " + dataAnnotation.bitOffset());
                log.trace("		size: " + dataAnnotation.size());
                log.trace("		arraySize: " + dataAnnotation.arraySize());

                final int offset = dataAnnotation.byteOffset();

                // update max offset
                if (offset > res.blockSize) {
                    res.blockSize = offset;
                }

                if (dataAnnotation.type() == S7Type.STRUCT) {
                    // recurse
                    log.trace("Recursing...");
                    final BeanParseResult subResult = parse(field.getType());
                    res.blockSize += subResult.blockSize;
                    log.trace("	New blocksize: " + res.blockSize);
                }

                log.trace("	New blocksize (+offset): " + res.blockSize);

                // Add dynamic size
                res.blockSize += dataAnnotation.size();

                // Plain element
                final BeanEntry entry = new BeanEntry();
                entry.byteOffset = dataAnnotation.byteOffset();
                entry.bitOffset = dataAnnotation.bitOffset();
                entry.field = field;
                entry.type = getWrapperForPrimitiveType(field.getType());
                entry.size = dataAnnotation.size();
                entry.s7type = dataAnnotation.type();
                entry.isArray = field.getType().isArray();
                entry.arraySize = dataAnnotation.arraySize();

                if (entry.isArray) {
                    entry.type = getWrapperForPrimitiveType(entry.type.getComponentType());
                }

                // Create new serializer
                final S7Serializable s = entry.s7type.getSerializer().getDeclaredConstructor().newInstance();
                entry.serializer = s;

                res.blockSize += (s.getSizeInBytes() * dataAnnotation.arraySize());
                log.trace("	New blocksize (+array): " + res.blockSize);

                if (s.getSizeInBits() > 0) {
                    boolean offsetOfBitAlreadyKnown = false;
                    for (final BeanEntry parsedEntry : res.entries) {
                        if (parsedEntry.byteOffset == entry.byteOffset) {
                            offsetOfBitAlreadyKnown = true;
                        }
                    }
                    if (!offsetOfBitAlreadyKnown) {
                        res.blockSize++;
                    }
                }

                res.entries.add(entry);
            }
        }

        log.trace("Parsing done, overall size: " + res.blockSize);

        return res;
    }

    /**
     * Parses an Object
     *
     * @param obj Object
     * @return BeanParseResult
     * @throws Exception Exception
     */
    public static BeanParseResult parse(final Object obj) throws Exception {
        return parse(obj.getClass());
    }

}
