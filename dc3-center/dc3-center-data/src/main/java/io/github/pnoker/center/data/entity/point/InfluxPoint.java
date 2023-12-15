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

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import lombok.Data;

@Data
@Measurement(name = "point_value")
public class InfluxPoint {

    @Column(tag = true)
    private Long deviceId;

    @Column(tag = true)
    private Long pointId;

    @Column
    private String pointValue;

    @Column(timestamp = true)
    private Long time;

    public InfluxPoint(PointValue pointValue) {
        this.deviceId = pointValue.getDeviceId();
        this.pointId = pointValue.getPointId();
        this.pointValue = pointValue.getValue();
        this.time = LocalDateTimeUtil.milliSeconds(pointValue.getCreateTime());
    }
}
