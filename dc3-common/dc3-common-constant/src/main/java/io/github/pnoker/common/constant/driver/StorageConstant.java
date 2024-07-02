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

package io.github.pnoker.common.constant.driver;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SuffixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;

/**
 * 存储 相关常量
 *
 * @author pnoker
 * @since 2022.1.0
 */
public class StorageConstant {

    /**
     * 设备数据存储集合前缀
     */
    public static final String POINT_VALUE_PREFIX = PrefixConstant.POINT + SuffixConstant.VALUE + SymbolConstant.UNDERSCORE;

    private StorageConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
