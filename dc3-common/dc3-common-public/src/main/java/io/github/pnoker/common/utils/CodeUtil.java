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

import cn.hutool.core.util.IdUtil;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;

/**
 * 编码生成工具类，提供唯一编码生成功能
 *
 * @author pnoker
 * @version 2024.3.10
 * @since 2022.1.0
 */
@Slf4j
public class CodeUtil {

    private CodeUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }


    /**
     * 生成一个简化的UUID作为唯一编码
     *
     * @return 返回一个简化的UUID字符串
     */
    public static String getCode() {
        return IdUtil.fastSimpleUUID();
    }
}
