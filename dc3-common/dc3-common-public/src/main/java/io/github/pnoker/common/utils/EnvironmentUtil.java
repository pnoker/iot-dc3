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

import io.github.pnoker.common.constant.common.EnvironmentConstant;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Node ID generation and environment tag builder.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
public class EnvironmentUtil {

    private EnvironmentUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Get a unique node ID.
     *
     * @return Node ID string
     */
    public static String getNodeId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get the topic tag. In the development environment, it is used to distinguish
     * multi-person development.
     *
     * @param env   Environment type
     * @param group Group identifier
     * @return Topic tag string
     */
    public static String getTag(String env, String group) {
        String exchangeTag = "";
        if (isDev(env)) {
            exchangeTag = env.toLowerCase() + SymbolConstant.UNDERSCORE + group.toLowerCase() + SymbolConstant.DOT;
        }
        return exchangeTag;
    }

    /**
     * Check whether the current environment is a development environment.
     *
     * @param env Environment type
     * @return true if it is a development environment, false otherwise
     */
    public static boolean isDev(String env) {
        return EnvironmentConstant.ENV_DEV.equals(env);
    }

}
