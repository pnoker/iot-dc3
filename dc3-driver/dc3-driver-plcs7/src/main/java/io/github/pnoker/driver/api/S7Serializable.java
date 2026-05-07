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
package io.github.pnoker.driver.api;

/**
 * Serialization interface for converting between Java types and S7 PLC byte buffers.
 *
 * @author Thomas Rudin
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface S7Serializable {

    /**
     * Extract a Java value from an S7 byte buffer.
     *
     * @param <T>         target type
     * @param targetClass the class to deserialize into
     * @param buffer      raw byte buffer from the PLC
     * @param byteOffset  byte offset within the buffer
     * @param bitOffset   bit offset within the byte
     * @return the deserialized value
     */
    <T> T extract(Class<T> targetClass, byte[] buffer, int byteOffset, int bitOffset);

    /**
     * @return the S7 type this serializer handles
     */
    S7Type getS7Type();

    /**
     * @return the size of this type in bits
     */
    int getSizeInBits();

    /**
     * @return the size of this type in bytes
     */
    int getSizeInBytes();

    /**
     * Insert a Java value into an S7 byte buffer.
     *
     * @param javaType   the value to serialize
     * @param buffer     target byte buffer
     * @param byteOffset byte offset within the buffer
     * @param bitOffset  bit offset within the byte
     * @param size       number of elements (for arrays)
     */
    void insert(Object javaType, byte[] buffer, int byteOffset, int bitOffset, int size);

}
