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

package io.github.pnoker.common.constant.driver;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;

/**
 * 策略工厂 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class StrategyConstant {

    private StrategyConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 存储相关的策略工厂 相关常量
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    public static class Storage {

        public static final String REPOSITORY_PREFIX = "repository" + SymbolConstant.COLON;
        public static final String POSTGRES = "postgres";
        public static final String INFLUXDB = "influxdb";
        public static final String TDENGINE = "tdengine";
        public static final String OPENTSDB = "opentsdb";
        public static final String MONGODB = "mongodb";
        public static final String ELASTICSEARCH = "elasticsearch";

        private Storage() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

}
