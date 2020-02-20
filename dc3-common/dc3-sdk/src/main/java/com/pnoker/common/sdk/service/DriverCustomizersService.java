/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.common.sdk.service;

import com.pnoker.common.model.Point;
import com.pnoker.common.sdk.init.DeviceDriver;

import java.util.Map;

/**
 * 驱动接口，用于驱动接口实现
 *
 * @author pnoker
 */
public interface DriverCustomizersService {
    /**
     * 初始化驱动
     *
     */
    void initial();

    /**
     * 驱动本身存在定时器，用于定时采集数据和下发数据，该方法为用户自定义操作，系统根据配置定时执行
     */
    void schedule();

    /**
     * 读操作
     */
    void read(Map<String, String> driverInfo, Map<String, String> pointInfo, Point point);

    /**
     * 写操作
     */
    void write();

    /**
     * 提供http接口模式接收数据
     */
    void receive();

    /**
     * 设备状态
     */
    void status();
}
