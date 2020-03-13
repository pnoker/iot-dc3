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
package com.github.s7connector.api.annotation;

import com.github.s7connector.api.S7Type;

import java.lang.annotation.*;

/**
 * Defines an Offset in a DB
 *
 * @author Thomas Rudin
 */
@Target(value = {ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface S7Variable {
    /**
     * The size of the array
     */
    int arraySize() default 1;

    /**
     * The bit offset, if any
     */
    int bitOffset() default 0;

    /**
     * The Byte Offset
     */
    int byteOffset();

    /**
     * The specified size (for String)
     */
    int size() default 0;

    /**
     * The corresponding S7 Type
     */
    S7Type type();

}
