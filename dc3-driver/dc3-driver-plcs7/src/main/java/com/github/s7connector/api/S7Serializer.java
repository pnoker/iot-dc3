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
package com.github.s7connector.api;

import com.dc3.driver.bean.Plcs7PointVariable;
import com.github.s7connector.exception.S7Exception;

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
     * @return
     * @throws S7Exception the s7 exception
     */
    Object dispense(Plcs7PointVariable plcs7PointVariable) throws S7Exception;

    /**
     * Stores an Object to the Datablock.
     *
     * @param bean       the bean
     * @param dbNum      the db num
     * @param byteOffset the byte offset
     */
    void store(Object bean, int dbNum, int byteOffset);

}