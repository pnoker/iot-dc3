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
package io.github.pnoker.driver.api.impl.serializer;

import io.github.pnoker.driver.api.DaveArea;
import io.github.pnoker.driver.api.S7Connector;
import io.github.pnoker.driver.api.S7Serializer;
import io.github.pnoker.driver.api.impl.serializer.parser.BeanEntry;
import io.github.pnoker.driver.api.impl.serializer.parser.BeanParseResult;
import io.github.pnoker.driver.api.impl.serializer.parser.BeanParser;
import io.github.pnoker.driver.bean.PlcS7PointVariable;
import io.github.pnoker.driver.exception.S7Exception;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;

/**
 * The Class S7Serializer is responsible for serializing S7 TCP Connection
 */
@Slf4j
public final class S7SerializerImpl implements S7Serializer {

    /**
     * The Connector.
     */
    private final S7Connector connector;

    /**
     * Instantiates a new s7 serializer.
     *
     * @param connector the connector
     */
    public S7SerializerImpl(final S7Connector connector) {
        this.connector = connector;
    }

    public static Object extractBytes(PlcS7PointVariable plcs7PointVariable, final byte[] buffer, final int byteOffset) {
        try {
            final BeanEntry entry = BeanParser.parse(plcs7PointVariable);
            return entry.serializer.extract(entry.type, buffer, entry.byteOffset + byteOffset, entry.bitOffset);
        } catch (Exception e) {
            throw new S7Exception("extractBytes", e);
        }
    }

    /**
     * Extracts bytes from a buffer.
     *
     * @param <T>        the generic type
     * @param beanClass  the bean class
     * @param buffer     the buffer
     * @param byteOffset the byte offset
     * @return the t
     */
    public static <T> T extractBytes(final Class<T> beanClass, final byte[] buffer, final int byteOffset) {
        log.trace("Extracting type {} from buffer with size: {} at offset {}", beanClass.getName(), buffer.length, byteOffset);

        try {
            final T obj = beanClass.newInstance();
            final BeanParseResult result = BeanParser.parse(beanClass);
            for (final BeanEntry entry : result.entries) {
                Object value = null;
                if (entry.isArray) {
                    value = Array.newInstance(entry.type, entry.arraySize);
                    for (int i = 0; i < entry.arraySize; i++) {
                        final Object component = entry.serializer.extract(entry.type, buffer,
                                entry.byteOffset + byteOffset + (i * entry.s7type.getByteSize()),
                                entry.bitOffset + (i * entry.s7type.getBitSize()));
                        Array.set(value, i, component);
                    }
                } else {
                    value = entry.serializer.extract(entry.type, buffer, entry.byteOffset + byteOffset, entry.bitOffset);
                }

                if (entry.field.getType() == byte[].class) {
                    //Special case issue #45
                    Byte[] oldValue = (Byte[]) value;

                    value = new byte[oldValue.length];

                    for (int i = 0; i < oldValue.length; i++) {
                        ((byte[]) value)[i] = oldValue[i];
                    }
                }

                entry.field.set(obj, value);
            }

            return obj;
        } catch (final Exception e) {
            throw new S7Exception("extractBytes", e);
        }
    }

    /**
     * Inserts the bytes to the buffer.
     *
     * @param bean       the bean
     * @param buffer     the buffer
     * @param byteOffset the byte offset
     */
    public static void insertBytes(final Object bean, final byte[] buffer, final int byteOffset) {
        log.trace("Inerting buffer with size: {} at offset {} into bean: {}", buffer.length, byteOffset, bean);

        try {
            final BeanParseResult result = BeanParser.parse(bean);

            for (final BeanEntry entry : result.entries) {
                final Object fieldValue = entry.field.get(bean);

                if (fieldValue != null) {
                    if (entry.isArray) {
                        for (int i = 0; i < entry.arraySize; i++) {
                            final Object arrayItem = Array.get(fieldValue, i);

                            if (arrayItem != null) {
                                entry.serializer.insert(arrayItem, buffer,
                                        entry.byteOffset + byteOffset + (i * entry.s7type.getByteSize()),
                                        entry.bitOffset + (i * entry.s7type.getBitSize()), entry.size);
                            }
                        }
                    } else {
                        entry.serializer.insert(fieldValue, buffer, entry.byteOffset + byteOffset, entry.bitOffset,
                                entry.size);
                    }
                }
            }
        } catch (final Exception e) {
            throw new S7Exception("insertBytes", e);
        }
    }

    @Override
    public synchronized <T> T dispense(final Class<T> beanClass, final int dbNum, final int byteOffset) throws S7Exception {
        try {
            final BeanParseResult result = BeanParser.parse(beanClass);
            final byte[] buffer = this.connector.read(DaveArea.DB, dbNum, result.blockSize, byteOffset);
            return extractBytes(beanClass, buffer, 0);
        } catch (final Exception e) {
            throw new S7Exception("dispense", e);
        }
    }

    @Override
    public synchronized <T> T dispense(final Class<T> beanClass, final int dbNum, final int byteOffset, final int blockSize) throws S7Exception {
        try {
            final byte[] buffer = this.connector.read(DaveArea.DB, dbNum, blockSize, byteOffset);
            return extractBytes(beanClass, buffer, 0);
        } catch (final Exception e) {
            throw new S7Exception("dispense dbnum(" + dbNum + ") byteoffset(" + byteOffset + ") blocksize(" + blockSize + ")", e);
        }
    }

    /**
     * add by pnoker
     */
    @Override
    public Object dispense(PlcS7PointVariable plcs7PointVariable) throws S7Exception {
        try {
            final byte[] buffer = this.connector.read(DaveArea.DB, plcs7PointVariable.getDbNum(), plcs7PointVariable.getSize(), plcs7PointVariable.getByteOffset());
            return extractBytes(plcs7PointVariable, buffer, 0);
        } catch (final Exception e) {
            throw new S7Exception("dispense dbnum(" + plcs7PointVariable.getDbNum() + ") byteoffset(" + plcs7PointVariable.getByteOffset() + ") blocksize(" + plcs7PointVariable.getSize() + ")", e);
        }
    }

    @Override
    public synchronized void store(final Object bean, final int dbNum, final int byteOffset) {
        try {
            final BeanParseResult result = BeanParser.parse(bean);

            final byte[] buffer = new byte[result.blockSize];
            log.trace("store-buffer-size: " + buffer.length);

            insertBytes(bean, buffer, 0);

            this.connector.write(DaveArea.DB, dbNum, byteOffset, buffer);
        } catch (final Exception e) {
            throw new S7Exception("store", e);
        }
    }

}
