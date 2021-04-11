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

package com.dc3.common.sdk.service;

import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.bean.driver.AttributeInfo;

import java.util.Map;

/**
 * <p>自定义驱动接口，开发的自定义驱动需要实现 read 和 write 接口，可以参考以提供的驱动模块写法</p>
 *
 * <ol>
 * <li>{@link DriverCustomService#initial} 初始化操作，需要根据不同的驱动实现该功能</li>
 * <li>{@link DriverCustomService#read} 读操作，需要根据不同的驱动实现该功能</li>
 * <li>{@link DriverCustomService#write} 写操作，需要根据不同的驱动实现该功能</li>
 * <li>{@link DriverCustomService#schedule} 调度操作，需要根据不同的驱动实现该功能</li>
 * </ol>
 *
 * @author pnoker
 */
public interface DriverCustomService {
    /**
     * Initial Driver
     */
    void initial();

    /**
     * Read Operation
     *
     * @param driverInfo Driver Attribute Info
     * @param pointInfo  Point Attribute Info
     * @param device     Device
     * @param point      Point
     * @return String Value
     * @throws Exception Exception
     */
    String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) throws Exception;

    /**
     * Write Operation
     *
     * @param driverInfo Driver Attribute Info
     * @param pointInfo  Point Attribute Info
     * @param device     Device
     * @param value      Value Attribute Info
     * @return Boolean Boolean
     * @throws Exception Exception
     */
    Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) throws Exception;

    /**
     * Schedule Operation
     */
    void schedule();
}
