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

package io.github.pnoker.common.exception;

import io.github.pnoker.common.utils.ExceptionUtil;

/**
 * 自定义 超出范围 异常
 *
 * @author pnoker
 * @since 2022.1.0
 */
public class OutRangeException extends RuntimeException {

    /**
     * OutRangeException
     */
    public OutRangeException() {
        this(null);
    }

    /**
     * OutRangeException
     *
     * @param template 文本模板，被替换的部分用 {} 表示，如果模板为null，返回"null"
     * @param params   参数值
     */
    public OutRangeException(CharSequence template, Object... params) {
        super(ExceptionUtil.getOutRangeMessage(template, params));
    }
}
