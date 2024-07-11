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

package io.github.pnoker.common.driver.service;

import io.github.pnoker.common.driver.entity.bean.RValue;
import io.github.pnoker.common.driver.entity.bean.WValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;

import java.util.Map;

/**
 * 自定义驱动接口, 开发的自定义驱动至少需要实现 read 和 write 接口, 可以参考以提供的驱动模块写法
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface DriverCustomService {
    /**
     * 初始化接口
     * <p>
     * 会在驱动启动时执行
     */
    void initial();

    /**
     * 自定义调度接口
     * <p>
     * 配置文件 driver.schedule.custom 进行配置
     */
    void schedule();

    /**
     * 驱动, 设备, 位号元数据事件
     * <p>
     * 驱动, 设备, 位号元数据新增, 更新, 删除都会触发改事件,
     * 需要根据数据类型{@link io.github.pnoker.common.enums.MetadataTypeEnum}决定是驱动, 设备, 位号
     *
     * @param metadataEvent 设备事件{@link MetadataEventDTO}
     */
    void event(MetadataEventDTO metadataEvent);

    /**
     * 读操作
     * <p>
     * 请灵活运行, 有些类型设备不一定能直接读取数据
     *
     * @param driverConfig 驱动属性配置
     * @param pointConfig  位号属性配置
     * @param device       设备
     * @param point        位号
     * @return 以字符串形式返回读取的值, 也存在抛异常
     */
    RValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point);

    /**
     * 写操作
     * <p>
     * 请灵活运行, 有些类型设备不一定能直接写入数据
     *
     * @param driverConfig 驱动属性配置
     * @param pointConfig  位号属性配置
     * @param device       设备
     * @param wValue       待写入数据
     * @return 是否写入, 也存在抛异常
     */
    Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue);

}
