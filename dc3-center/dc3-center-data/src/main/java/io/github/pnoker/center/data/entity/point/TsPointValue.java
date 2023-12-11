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

package io.github.pnoker.center.data.entity.point;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenTSDB 位号数据
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
public class TsPointValue implements Serializable {
    private String metric;
    private Long timestamp;
    private Object value;
    private Map<String, String> tags = new HashMap<>(4);

    public TsPointValue addTag(String tagName, String tagValue) {
        this.tags.put(tagName, tagValue);
        return this;
    }

    public TsPointValue(String metric, String value) {
        this.metric = metric;
        this.timestamp = System.currentTimeMillis();
        this.value = value;
    }
}
