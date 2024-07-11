/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.constant.common;

/**
 * 默认 相关常量
 *
 * @author pnoker
 * @since 2022.1.0
 */
public class DefaultConstant {

    /**
     * 默认 Integer 空值: -1
     */
    public static final Integer NULL_INT = -1;
    /**
     * 零: 0
     */
    public static final Integer ZERO = 0;
    /**
     * 一: 1
     */
    public static final Integer ONE = 1;
    /**
     * 默认 String 空值: nil
     */
    public static final String NULL_STRING = "nil";
    /**
     * 默认分页数
     */
    public static final Integer PAGE_SIZE = 20;
    /**
     * 默认最大分页数
     */
    public static final Integer MAX_PAGE_SIZE = 100;

    private DefaultConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
