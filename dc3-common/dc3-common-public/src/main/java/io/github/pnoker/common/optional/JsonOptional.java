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

package io.github.pnoker.common.optional;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;

import java.util.function.Consumer;

/**
 * 自定义 String Optional
 *
 * @author pnoker
 * @since 2022.1.0
 */
public final class JsonOptional {

    private final String value;

    private JsonOptional(String value) {
        this.value = value;
    }

    public static JsonOptional ofNullable(String value) {
        return new JsonOptional(value);
    }

    public void ifPresent(Consumer<String> action) {
        if (CharSequenceUtil.isNotEmpty(value) && JSONUtil.isTypeJSON(value)) {
            action.accept(value);
        }
    }

    public void ifPresentOrElse(Consumer<String> action, Runnable emptyAction) {
        if (CharSequenceUtil.isNotEmpty(value) && JSONUtil.isTypeJSON(value)) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }
}
