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
 * 服务相关
 *
 * @author pnoker
 */
public interface ServiceConstant {

    /**
     * 规则引擎服务相关
     */
    interface Ekuiper {
        String SERVICE_NAME = "DC3-CENTER-EKUIPER";
        String STREAM_URL_PREFIX = "/stream";
        String TABLE_URL_PREFIX = "/table";
        String RULE_URL_PREFIX = "/rule";
        String CONFKEY_URL_PREFIX = "/confkey";
        String SINKTEM_URL_PREFIX = "/sinktem";
    }

}
