/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.bean.driver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DeviceEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 设备ID，同MySQl中等 设备ID 一致
     */
    private Long deviceId;

    /**
     * 位号ID，同MySQl中等 位号ID 一致
     */
    private Long pointId;

    /**
     * Device Event
     * <p>
     * STATUS、LIMIT
     */
    private String type;

    private Boolean confirm = false;
    private Object content;
    private Long originTime;

    public DeviceEvent(Long deviceId, String type, Object content) {
        this.deviceId = deviceId;
        this.type = type;
        this.content = content;
        this.originTime = System.currentTimeMillis();
    }

    public DeviceEvent(Long deviceId, Long pointId, String type, Object content) {
        this.deviceId = deviceId;
        this.pointId = pointId;
        this.type = type;
        this.content = content;
        this.originTime = System.currentTimeMillis();
    }
}
