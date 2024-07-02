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
import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;

/**
 * 异常 相关工具类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
public class ExceptionUtil {

    private ExceptionUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 获取不可用服务的错误信息
     *
     * @param service 服务名称
     * @param message 默认的错误信息
     * @return 错误信息
     */
    public static String getNotAvailableServiceMessage(String service, String message) {
        if (CharSequenceUtil.isEmpty(message)) {
            message = CharSequenceUtil.format("{}: {}", ExceptionConstant.NO_AVAILABLE_SERVER, service);
        }
        return message;
    }
}
