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

package com.dc3.common.sdk.service;

import com.dc3.common.bean.driver.PointValue;

/**
 * @author pnoker
 */
public interface DriverCommandService {

    /**
     * 读操作
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @return PointValue
     */
    PointValue read(Long deviceId, Long pointId);

    /**
     * 写操作
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @param value    String Value
     * @return Boolean
     */
    Boolean write(Long deviceId, Long pointId, String value);

}
