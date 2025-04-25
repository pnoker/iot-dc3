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

package io.github.pnoker.driver.service.impl;

import cn.hutool.core.util.RandomUtil;
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
import java.util.concurrent.TimeUnit;

/**
 * 驱动自定义服务实现类
 *
 * @author pnoker
 * @version 2025.2.5
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
         * 驱动初始化逻辑
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 驱动启动时会自动执行该方法，您可以在此处执行特定的初始化操作。
         *
         */
    }

    @Override
    public void schedule() {
        /*
         * 设备状态上传逻辑
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 设备状态的上传可以根据具体需求灵活实现，以下是一些常见的实现方式：
         * - 在 `read` 方法中根据读取的数据判断设备状态；
         * - 在自定义的定时任务中定期检查设备状态；
         * - 根据特定的业务逻辑或事件触发设备状态的判断。
         *
         * 最终通过 {@link DriverSenderService#deviceStatusSender(Long, DeviceStatusEnum)} 接口将设备状态提交给 SDK 管理。
         * 设备状态枚举 {@link DeviceStatusEnum} 包含以下状态：
         * - ONLINE: 设备在线
         * - OFFLINE: 设备离线
         * - MAINTAIN: 设备维护中
         * - FAULT: 设备故障
         *
         * 在以下示例中，所有设备的状态被设置为 {@link DeviceStatusEnum#ONLINE}，并设置状态的有效期为 25 {@link TimeUnit#SECONDS}。
         */
        driverMetadata.getDeviceIds().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * 接收驱动、设备、位号元数据的新增、更新、删除事件。
         *
         * 元数据类型: {@link MetadataTypeEnum} (DRIVER, DEVICE, POINT)
         * 元数据操作类型: {@link MetadataOperateTypeEnum} (ADD, DELETE, UPDATE)
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
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
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 根据点位类型生成随机数据：
         * - 如果点位类型为字符串类型，生成一个长度为8的随机字符串；
         * - 如果点位类型为布尔类型，生成一个随机的布尔值；
         * - 其他情况下，生成一个0到100之间的随机浮点数。
         */
        if (PointTypeFlagEnum.STRING.equals(point.getPointTypeFlag())) {
            return new RValue(device, point, RandomUtil.randomString(8));
        }
        if (PointTypeFlagEnum.BOOLEAN.equals(point.getPointTypeFlag())) {
            return new RValue(device, point, String.valueOf(RandomUtil.randomBoolean()));
        }

        return new RValue(device, point, String.valueOf(RandomUtil.randomDouble(100)));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
         * 写入设备点位数据逻辑
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 您可以根据具体的业务需求实现点位数据的写入逻辑。
         * 默认情况下，该方法返回 false，表示写入操作未执行或失败。
         */
        return false;
    }

}
