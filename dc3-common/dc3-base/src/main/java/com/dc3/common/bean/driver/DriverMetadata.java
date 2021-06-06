/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.bean.driver;

import com.dc3.common.model.Device;
import com.dc3.common.model.DriverAttribute;
import com.dc3.common.model.Point;
import com.dc3.common.model.PointAttribute;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Driver Metadata
 *
 * @author pnoker
 */
@Data
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DriverMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<Long, DriverAttribute> driverAttributeMap;
    private Map<Long, PointAttribute> pointAttributeMap;

    /**
     * deviceId(driverAttribute.name,(driverInfo.value,driverAttribute.type))
     */
    private Map<Long, Map<String, AttributeInfo>> driverInfoMap;

    /**
     * deviceId,device
     */
    private Map<Long, Device> deviceMap;

    /**
     * profileId(pointId,point)
     */
    private Map<Long, Map<Long, Point>> pointMap;

    /**
     * deviceId(pointId(pointAttribute.name,(pointInfo.value,pointAttribute.type)))
     */
    private Map<Long, Map<Long, Map<String, AttributeInfo>>> pointInfoMap;

    public DriverMetadata() {
        this.driverAttributeMap = new ConcurrentHashMap<>(16);
        this.pointAttributeMap = new ConcurrentHashMap<>(16);
        this.driverInfoMap = new ConcurrentHashMap<>(16);
        this.deviceMap = new ConcurrentHashMap<>(16);
        this.pointMap = new ConcurrentHashMap<>(16);
        this.pointInfoMap = new ConcurrentHashMap<>(16);
    }
}
