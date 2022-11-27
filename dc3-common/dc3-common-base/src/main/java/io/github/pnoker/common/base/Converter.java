/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.base;

/**
 * DO、DTO对象相互转换接口，DTO对象请实现该接口
 *
 * @param <D> DO
 * @author pnoker
 * @since 2022.1.0
 */
public interface Converter<D> {
    /**
     * DTO 转 DO
     *
     * @param d Do Object
     */
    void convertDtoToDo(D d);

    /**
     * DO 转 DTO
     *
     * @param d Do Object
     */
    void convertDoToDto(D d);
}
