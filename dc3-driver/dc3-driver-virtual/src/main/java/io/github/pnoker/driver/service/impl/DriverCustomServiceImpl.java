/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.RValue;
import io.github.pnoker.common.driver.entity.bean.WValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 驱动自定义服务实现类
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    DriverMetadata driverMetadata;
    @Resource
    private DriverSenderService driverSenderService;

    @Override
    public void initial() {
        /*
         * Driver initialization logic
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * This method is automatically executed when the driver starts, and you can perform specific initialization operations here.
         *
         */
    }

    @Override
    public void schedule() {
        /*
         * Device status upload logic
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * Device status upload can be flexibly implemented based on specific requirements. Here are some common approaches:
         * - Determine device status based on read data in the `read` method;
         * - Periodically check device status in a custom scheduled task;
         * - Trigger device status judgment based on specific business logic or events.
         *
         * Finally, submit the device status to the SDK management through the {@link DriverSenderService#deviceStatusSender(Long, DeviceStatusEnum)} interface.
         * The device status enumeration {@link DeviceStatusEnum} includes the following states:
         * - ONLINE: Device online
         * - OFFLINE: Device offline
         * - MAINTAIN: Device under maintenance
         * - FAULT: Device fault
         *
         * In the following example, all devices are set to {@link DeviceStatusEnum#ONLINE}, with a status validity period of 25 {@link TimeUnit#SECONDS}.
         */
        driverMetadata.getDeviceIds().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * 接收驱动, 设备, 位号元数据的新增, 更新, 删除事件。
         *
         * 元数据类型: {@link MetadataTypeEnum} (DRIVER, DEVICE, POINT)
         * 元数据操作类型: {@link MetadataOperateTypeEnum} (ADD, DELETE, UPDATE)
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         */
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            // to do something for device event
            log.info("Device metadata event: deviceId: {}, operate: {}", metadataEvent.getId(), operateType);
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            // to do something for point event
            log.info("Point metadata event: pointId: {}, operate: {}", metadataEvent.getId(), operateType);
        }
    }

    @Override
    public RValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point) {
        /*
         * 读取设备点位数据逻辑
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         * 根据点位类型生成随机数据：
         * - 如果点位类型为字符串类型, 生成一个长度为8的随机字符串；
         * - 如果点位类型为布尔类型, 生成一个随机的布尔值；
         * - 其他情况下, 生成一个0到100之间的随机浮点数。
         */
        if (PointTypeFlagEnum.STRING.equals(point.getPointTypeFlag())) {
            return new RValue(device, point, "abcd1234");
        }
        if (PointTypeFlagEnum.BOOLEAN.equals(point.getPointTypeFlag())) {
            Random random = new Random();
            boolean b = random.nextBoolean();
            return new RValue(device, point, String.valueOf(b));
        }

        Random random = new Random();
        double value = random.nextDouble() * 100;
        return new RValue(device, point, String.valueOf(value));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
         * 写入设备点位数据逻辑
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         * 您可以根据具体的业务需求实现点位数据的写入逻辑。
         * 默认情况下, 该方法返回 false, 表示写入操作未执行或失败。
         */
        return false;
    }

}
