/*
Copyright 2016 S7connector members (github.com/s7connector)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.github.s7connector.impl.serializer;

import com.dc3.driver.bean.Plcs7PointVariable;
import com.github.s7connector.api.DaveArea;
import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.S7Serializer;
import com.github.s7connector.exception.S7Exception;
import com.github.s7connector.impl.serializer.parser.BeanEntry;
import com.github.s7connector.impl.serializer.parser.BeanParseResult;
import com.github.s7connector.impl.serializer.parser.BeanParser;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;

/**
 * The Class S7Serializer is responsible for serializing S7 TCP Connection
 */
@Slf4j
public final class S7SerializerImpl implements S7Serializer {

    public static Object extractBytes(Plcs7PointVariable plcs7PointVariable, final byte[] buffer, final int byteOffset) {
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
    public Object dispense(Plcs7PointVariable plcs7PointVariable) throws S7Exception {
        try {
            final byte[] buffer = this.connector.read(DaveArea.DB, plcs7PointVariable.getDbNum(), plcs7PointVariable.getSize(), plcs7PointVariable.getByteOffset());
            return extractBytes(plcs7PointVariable, buffer, 0);
        } catch (final Exception e) {
            throw new S7Exception("dispense dbnum(" + plcs7PointVariable.getDbNum() + ") byteoffset(" + plcs7PointVariable.getByteOffset() + ") blocksize(" + plcs7PointVariable.getSize() + ")", e);
        }
    }

    /**
     * {@inheritDoc}
     */
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
