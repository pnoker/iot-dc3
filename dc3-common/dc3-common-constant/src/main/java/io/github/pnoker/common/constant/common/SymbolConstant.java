/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.constant.common;

/**
 * 符号 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
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
