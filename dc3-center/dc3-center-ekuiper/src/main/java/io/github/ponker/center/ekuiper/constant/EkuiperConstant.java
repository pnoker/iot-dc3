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

package io.github.ponker.center.ekuiper.constant;

/**
 * @author : Zhen
 * <p>
 * 规则引擎相关
 */
public interface EkuiperConstant {

    /**
     * 规则引擎url相关
     */
    interface EKUIPERURL {

        String STREAM_URL = "http://localhost:9081/streams";
        String TABLE_URL = "http://localhost:9081/tables";
        String CONFKEY_URL = "http://localhost:9081/metadata/sources";
        String RULE_URL = "http://localhost:9081/rules";
    }


    interface ConKeyType {

        String MQTT = "mqtt";
        String REDIS = "redis";
        String HTTPPULL = "httppull";
        String HTTPPUSH = "httppush";
        String SQL = "sql";
        String REST = "rest";
    }

}
