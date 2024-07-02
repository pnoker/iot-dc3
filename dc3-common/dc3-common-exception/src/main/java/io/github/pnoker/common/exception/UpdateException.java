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

package io.github.pnoker.common.exception;

import cn.hutool.core.text.CharSequenceUtil;

/**
 * 自定义更新异常
 *
 * @author pnoker
 * @since 2022.1.0
 */
public class UpdateException extends RuntimeException {
    public UpdateException() {
        this(null);
    }

    public UpdateException(Throwable cause) {
        super(cause);
    }

    public UpdateException(CharSequence template, Object... params) {
        super(CharSequenceUtil.format(template, params));
    }
}