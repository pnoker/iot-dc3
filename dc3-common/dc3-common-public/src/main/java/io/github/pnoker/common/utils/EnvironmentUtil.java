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

package io.github.pnoker.common.utils;

import cn.hutool.core.util.RandomUtil;
import io.github.pnoker.common.constant.common.EnvironmentConstant;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import lombok.extern.slf4j.Slf4j;

/**
 * 环境相关的工具类集合
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
public class EnvironmentUtil {

    private EnvironmentUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 获取节点 ID
     *
     * @return R of String Suffix
     */
    public static String getNodeId() {
        return RandomUtil.randomString(8).toLowerCase();
    }

    /**
     * 获取 Topic Tag
     * 开发环境用于区分多人开发
     *
     * @param env   环境类型
     * @param group 分组
     * @return R of String Tag
     */
    public static String getTag(String env, String group) {
        String exchangeTag = "";
        if (isDev(env)) {
            exchangeTag = env.toLowerCase() + SymbolConstant.UNDERSCORE + group.toLowerCase() + SymbolConstant.DOT;
        }
        return exchangeTag;
    }

    /**
     * 是否为开发环境
     *
     * @param env 环境类型
     * @return 是/否
     */
    public static boolean isDev(String env) {
        return EnvironmentConstant.ENV_DEV.equals(env);
    }

}
