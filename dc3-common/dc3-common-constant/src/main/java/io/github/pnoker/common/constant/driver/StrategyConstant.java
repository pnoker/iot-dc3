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

package io.github.pnoker.common.constant.driver;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;

/**
 * 策略工厂 相关常量
 *
 * @author pnoker
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
     * @since 2022.1.0
     */
    public static class Storage {

        public static final String REPOSITORY_PREFIX = "storage" + SymbolConstant.COLON;
        public static final String MONGO = "mongo";
        public static final String INFLUXDB = "influxdb";
        public static final String STRATEGY_OPENTSDB = "opentsdb";
        public static final String STRATEGY_ELASTICSEARCH = "elasticsearch";
        public static final String TDENGINE = "tdengine";

        private Storage() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

}
