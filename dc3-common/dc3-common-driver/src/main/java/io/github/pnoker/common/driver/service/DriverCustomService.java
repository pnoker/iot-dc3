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
 * 自定义驱动服务接口
 * <p>
 * 开发自定义驱动时，至少需要实现 {@link #read} 和 {@link #write} 方法。
 * 可以参考已提供的驱动模块实现方式。
 *
 * @author pnoker
 * @version 2025.2.4
 * @since 2022.1.0
 */
public interface DriverCustomService {
    /**
     * 驱动初始化接口
     * <p>
     * 该接口在驱动启动时自动调用，用于执行驱动所需的初始化操作。
     * 开发者可以在此方法中配置驱动启动时的必要资源或进行环境准备。
     */
    void initial();

    /**
     * 自定义调度接口
     * <p>
     * 该接口用于执行自定义调度任务，可通过配置文件 {@code driver.schedule.custom} 进行相关配置。
     * 开发者可以在此方法中实现自定义的调度逻辑，例如定时任务、周期性任务等。
     * <p>
     * 注意: 调度任务的执行频率、触发条件等应根据实际需求进行合理配置。
     */
    void schedule();

    /**
     * 处理驱动、设备、位号的元数据事件
     * <p>
     * 当驱动、设备或位号的元数据发生新增、更新或删除操作时，将触发此事件。
     * 具体的事件类型（驱动、设备或位号）由 {@link io.github.pnoker.common.enums.MetadataTypeEnum} 决定。
     *
     * @param metadataEvent 元数据事件对象，包含事件相关的详细信息 {@link MetadataEventDTO}
     */
    void event(MetadataEventDTO metadataEvent);

    /**
     * 执行读操作
     * <p>
     * 该接口用于从指定设备中读取位号的数据。由于设备类型和通信协议的差异，读取操作可能无法直接执行，请根据实际情况灵活处理。
     * <p>
     * 注意: 读取操作可能会抛出异常，调用方需做好异常处理。
     *
     * @param driverConfig 驱动属性配置，包含驱动相关的配置信息
     * @param pointConfig  位号属性配置，包含位号相关的配置信息
     * @param device       设备对象，包含设备的基本信息和属性
     * @param point        位号对象，包含位号的基本信息和属性
     * @return 返回读取到的数据，封装在 {@link RValue} 对象中
     */
    RValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point);

    /**
     * 执行写操作
     * <p>
     * 该接口用于向指定设备中的位号写入数据。由于设备类型和通信协议的差异，写入操作可能无法直接执行，请根据实际情况灵活处理。
     * <p>
     * 注意: 写入操作可能会抛出异常，调用方需做好异常处理。
     *
     * @param driverConfig 驱动属性配置，包含驱动相关的配置信息
     * @param pointConfig  位号属性配置，包含位号相关的配置信息
     * @param device       设备对象，包含设备的基本信息和属性
     * @param point        位号对象，包含位号的基本信息和属性
     * @param wValue       待写入的数据，封装在 {@link WValue} 对象中
     * @return 返回写入操作是否成功，若成功则返回 {@code true}，否则返回 {@code false} 或抛出异常
     */
    Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue);

}
