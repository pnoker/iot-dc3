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

import io.github.pnoker.driver.bean.PlcS7PointVariable;
import io.github.pnoker.driver.exception.S7Exception;

/**
 * Interface for reading (dispense) and writing (store) typed data
 * to/from S7 PLC data blocks.
 *
 * @author Thomas Rudin
 */
public interface S7Serializer {

    /**
     * Deserialize an object from a data block.
     *
     * @param <T>        target type
     * @param beanClass  class to deserialize into
     * @param dbNum      data block number
     * @param byteOffset byte offset within the DB
     * @return the deserialized object
     * @throws S7Exception if reading fails
     */
    <T> T dispense(Class<T> beanClass, int dbNum, int byteOffset) throws S7Exception;

    /**
     * Deserialize an object from a data block with an explicit block size.
     *
     * @param <T>        target type
     * @param beanClass  class to deserialize into
     * @param dbNum      data block number
     * @param byteOffset byte offset within the DB
     * @param blockSize  number of bytes to read
     * @return the deserialized object
     * @throws S7Exception if reading fails
     */
    <T> T dispense(Class<T> beanClass, int dbNum, int byteOffset, int blockSize) throws S7Exception;

    /**
     * Read a point value from a data block using the point variable definition.
     *
     * @param plcs7PointVariable point variable with DB number, offsets, and type
     * @return the read value
     * @throws S7Exception if reading fails
     */
    Object dispense(PlcS7PointVariable plcs7PointVariable) throws S7Exception;

    /**
     * Serialize and write an object to a data block.
     *
     * @param bean       the value to write
     * @param dbNum      data block number
     * @param byteOffset byte offset within the DB
     */
    void store(Object bean, int dbNum, int byteOffset);

}