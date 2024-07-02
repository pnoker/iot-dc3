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
 * 符号 相关常量
 *
 * @author pnoker
 * @since 2022.1.0
 */
public class SymbolConstant {

    /**
     * 点
     */
    public static final String DOT = ".";
    /**
     * 下划线
     */
    public static final String UNDERSCORE = "_";
    /**
     * 连接线
     */
    public static final String HYPHEN = "-";
    /**
     * 星号
     */
    public static final String ASTERISK = "*";
    /**
     * 井号
     */
    public static final String HASHTAG = "#";
    /**
     * 冒号
     */
    public static final String COLON = ":";
    /**
     * 双冒号
     */
    public static final String DOUBLE_COLON = "::";
    /**
     * 斜线
     */
    public static final String SLASH = "/";
    /**
     * 双斜线
     */
    public static final String DOUBLE_SLASH = "//";

    private SymbolConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
