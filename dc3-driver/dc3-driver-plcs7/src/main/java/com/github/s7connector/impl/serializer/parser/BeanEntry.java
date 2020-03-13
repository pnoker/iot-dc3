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
package com.github.s7connector.impl.serializer.parser;

import com.github.s7connector.api.S7Serializable;
import com.github.s7connector.api.S7Type;

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
