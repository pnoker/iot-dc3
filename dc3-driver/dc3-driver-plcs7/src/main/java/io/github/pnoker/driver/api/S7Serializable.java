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
 * The Interface S7Serializable API
 *
 * @author Thomas Rudin
 */
public interface S7Serializable {

    /**
     * Extracts a java type from a byte buffer.
     *
     * @param <T>         the generic type
     * @param targetClass the target class
     * @param buffer      the buffer
     * @param byteOffset  the byte offset
     * @param bitOffset   the bit offset
     * @return the t
     */
    public <T> T extract(Class<T> targetClass, byte[] buffer, int byteOffset, int bitOffset);

    /**
     * Returns the S7-Type.
     *
     * @return the s7 type
     */
    public S7Type getS7Type();

    /**
     * Returns the size of the s7 type bytes.
     *
     * @return the size in bits
     */
    public int getSizeInBits();

    /**
     * Returns the size of the s7 type bytes.
     *
     * @return the size in bytes
     */
    public int getSizeInBytes();

    /**
     * Inserts a Java Object to the byte buffer.
     *
     * @param javaType   the java type
     * @param buffer     the buffer
     * @param byteOffset the byte offset
     * @param bitOffset  the bit offset
     * @param size       the size
     */
    public void insert(Object javaType, byte[] buffer, int byteOffset, int bitOffset, int size);
}
