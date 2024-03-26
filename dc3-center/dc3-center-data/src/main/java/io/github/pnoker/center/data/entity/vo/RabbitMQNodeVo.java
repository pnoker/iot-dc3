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

package io.github.pnoker.center.data.entity.vo;

import lombok.Data;

@Data
public class RabbitMQNodeVo {
    private Metric metric;
    private ValueItem value;


    @Data
    public static class Metric {
        private String erlangVersion;
        private String instance;
        private String job;
        private String prometheusClientVersion;
        private String prometheusPluginVersion;
        private String rabbitmqCluster;
        private String rabbitmqNode;
        private String rabbitmqVersion;
    }

    @Data
    public static class ValueItem {
        private String tValue;
        private String sValue;
    }
}
