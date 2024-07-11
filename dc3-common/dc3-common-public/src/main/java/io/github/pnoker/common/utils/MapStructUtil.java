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

package io.github.pnoker.common.utils;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Condition;

/**
 * MapStruct 工具类集合
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
public class MapStructUtil {

    private MapStructUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    @Condition
    public static boolean isNotEmpty(String value) {
        return !CharSequenceUtil.isNullOrUndefined(value);
    }

    @Condition
    public static boolean isValidNumber(Number value) {
        return NumberUtil.isValidNumber(value);
    }
}
