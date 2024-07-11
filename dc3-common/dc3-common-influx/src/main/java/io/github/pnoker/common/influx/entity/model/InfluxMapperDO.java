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

package io.github.pnoker.common.influx.entity.model;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Measurement(name = "dc3")
@Data
public class InfluxMapperDO {

    @Column(timestamp = true)
    private Instant time; //主键生成时间

    @Column(name = "deviceId", tag = true)
    private String deviceId;

    @Column(name = "pointId", tag = true)
    private String pointId;

    @Column(name = "originTime", tag = true)
    private String originTime;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "field")
    private String field;


}
