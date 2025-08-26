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
 * @author Thomas Rudin
 */
public interface S7Serializer {

    /**
     * Dispenses an Object from the mapping of the Datablock.
     *
     * @param <T>        the generic type
     * @param beanClass  the bean class
     * @param dbNum      the db num
     * @param byteOffset the byte offset
     * @return the t
     * @throws S7Exception the s7 exception
     */
    <T> T dispense(Class<T> beanClass, int dbNum, int byteOffset) throws S7Exception;

    /**
     * Dispense.
     *
     * @param <T>        the generic type
     * @param beanClass  the bean class
     * @param dbNum      the db num
     * @param byteOffset the byte offset
     * @param blockSize  the block size
     * @return the t
     * @throws S7Exception the s7 exception
     */
    <T> T dispense(Class<T> beanClass, int dbNum, int byteOffset, int blockSize) throws S7Exception;


    /**
     * Dispense.
     *
     * @param plcs7PointVariable the point
     * @return Object
     * @throws S7Exception the s7 exception
     */
    Object dispense(PlcS7PointVariable plcs7PointVariable) throws S7Exception;

    /**
     * Stores an Object to the Datablock.
     *
     * @param bean       the bean
     * @param dbNum      the db num
     * @param byteOffset the byte offset
     */
    void store(Object bean, int dbNum, int byteOffset);

}