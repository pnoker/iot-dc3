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

import io.github.pnoker.common.driver.entity.bean.RValue;
import io.github.pnoker.common.driver.entity.bean.WValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.driver.api.S7Connector;
import io.github.pnoker.driver.api.S7Serializer;
import io.github.pnoker.driver.api.factory.S7ConnectorFactory;
import io.github.pnoker.driver.api.factory.S7SerializerFactory;
import io.github.pnoker.driver.bean.PlcS7PointVariable;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


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

    /**
     * Plc Connector Map
     * 仅供参考
     */
    private Map<Long, MyS7Connector> connectMap;

    @Override
    public void initial() {
        /*
         * 驱动初始化逻辑
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         * 驱动启动时会自动执行该方法, 您可以在此处执行特定的初始化操作。
         *
         */
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        /*
         * 设备状态上传逻辑
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         * 设备状态的上传可以根据具体需求灵活实现, 以下是一些常见的实现方式：
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
         * 在以下示例中, 所有设备的状态被设置为 {@link DeviceStatusEnum#ONLINE}, 并设置状态的有效期为 25 {@link TimeUnit#SECONDS}。
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

            // When the device is updated or deleted, remove the corresponding connection handle
            if (MetadataOperateTypeEnum.DELETE.equals(operateType) || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                connectMap.remove(metadataEvent.getId());
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            // to do something for point event
            log.info("Point metadata event: pointId: {}, operate: {}", metadataEvent.getId(), operateType);
        }
    }

    @Override
    public RValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point) {
        /*
         * PLC S7 数据读取逻辑
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         * 该方法用于从 PLC S7 设备中读取指定点位的数据。
         * 1. 获取设备的 S7 连接器。
         * 2. 加锁以确保线程安全。
         * 3. 使用 S7 序列化器读取点位数据。
         * 4. 将读取到的数据封装为 RValue 对象返回。
         * 5. 捕获并记录异常, 确保锁在 finally 块中释放。
         */
        log.debug("Plc S7 Read, device: {}, point: {}", JsonUtil.toJsonString(device), JsonUtil.toJsonString(point));
        MyS7Connector myS7Connector = getS7Connector(device.getId(), driverConfig);

        try {
            myS7Connector.lock.writeLock().lock();
            S7Serializer serializer = S7SerializerFactory.buildSerializer(myS7Connector.getConnector());
            PlcS7PointVariable plcs7PointVariable = getPointVariable(pointConfig, point.getPointTypeFlag().getCode());
            return new RValue(device, point, String.valueOf(serializer.dispense(plcs7PointVariable)));
        } catch (Exception e) {
            log.error("Plc S7 Read Error: {}", e.getMessage());
            return null;
        } finally {
            myS7Connector.lock.writeLock().unlock();
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
         * PLC S7 数据写入逻辑
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         * 该方法用于向 PLC S7 设备写入指定点位的数据。
         * 1. 获取设备的 S7 连接器。
         * 2. 加锁以确保线程安全。
         * 3. 使用 S7 序列化器写入点位数据。
         * 4. 捕获并记录异常, 确保锁在 finally 块中释放。
         * 5. 返回写入操作的结果(成功或失败)。
         */
        log.debug("Plc S7 Write, device: {}, value: {}", JsonUtil.toJsonString(device), JsonUtil.toJsonString(wValue));
        MyS7Connector myS7Connector = getS7Connector(device.getId(), driverConfig);
        myS7Connector.lock.writeLock().lock();
        S7Serializer serializer = S7SerializerFactory.buildSerializer(myS7Connector.getConnector());
        PlcS7PointVariable plcs7PointVariable = getPointVariable(pointConfig, wValue.getType().getCode());

        try {
            store(serializer, plcs7PointVariable, wValue.getType().getCode(), wValue.getValue());
            return true;
        } catch (Exception e) {
            log.error("Plc S7 Write Error: {}", e.getMessage());
            return false;
        } finally {
            myS7Connector.lock.writeLock().unlock();
        }
    }

    /**
     * 获取 PLC S7 连接器
     * <p>
     * 该方法用于从缓存中获取指定设备的 S7 连接器。如果缓存中不存在该设备的连接器,
     * 则会根据驱动配置信息创建一个新的连接器, 并将其缓存以供后续使用。
     * <p>
     * 连接器创建过程中, 会从驱动配置中获取主机地址和端口号, 并初始化读写锁以确保线程安全。
     * 如果连接器创建失败, 将抛出 {@link ServiceException} 异常。
     *
     * @param deviceId     设备ID, 用于标识唯一的设备连接器
     * @param driverConfig 驱动配置信息, 包含连接 PLC 所需的主机地址和端口号等参数
     * @return 返回与设备ID对应的 {@link MyS7Connector} 对象, 包含 S7 连接器和读写锁
     * @throws ServiceException 如果连接器创建失败, 抛出此异常
     */
    private MyS7Connector getS7Connector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        MyS7Connector myS7Connector = connectMap.get(deviceId);
        if (Objects.isNull(myS7Connector)) {
            myS7Connector = new MyS7Connector();

            log.debug("Plc S7 Connection Info {}", JsonUtil.toJsonString(driverConfig));
            try {
                S7Connector s7Connector = S7ConnectorFactory.buildTCPConnector()
                        .withHost(driverConfig.get("host").getValue(String.class))
                        .withPort(driverConfig.get("port").getValue(Integer.class))
                        .build();
                myS7Connector.setLock(new ReentrantReadWriteLock());
                myS7Connector.setConnector(s7Connector);
            } catch (Exception e) {
                throw new ServiceException("new s7connector fail" + e.getMessage());
            }
            connectMap.put(deviceId, myS7Connector);
        }
        return myS7Connector;
    }

    /**
     * 获取 PLC S7 点位变量信息
     * <p>
     * 该方法用于从点位配置中提取 PLC S7 点位变量信息, 并封装为 {@link PlcS7PointVariable} 对象。
     * 点位配置中应包含以下关键属性：
     * - dbNum: 数据块编号
     * - byteOffset: 字节偏移量
     * - bitOffset: 位偏移量
     * - blockSize: 数据块大小
     * - type: 点位数据类型
     * <p>
     * 如果点位配置中缺少上述任一属性, 将抛出 {@link NullPointerException} 异常。
     *
     * @param pointConfig 点位配置信息, 包含点位变量的相关属性
     * @param type        点位数据类型, 用于标识点位数据的类型
     * @return 返回封装好的 {@link PlcS7PointVariable} 对象, 包含点位变量的详细信息
     * @throws NullPointerException 如果点位配置中缺少必要的属性, 抛出此异常
     */
    private PlcS7PointVariable getPointVariable(Map<String, AttributeBO> pointConfig, String type) {
        log.debug("Plc S7 Point Attribute Config {}", JsonUtil.toJsonString(pointConfig));
        return new PlcS7PointVariable(
                pointConfig.get("dbNum").getValue(Integer.class),
                pointConfig.get("byteOffset").getValue(Integer.class),
                pointConfig.get("bitOffset").getValue(Integer.class),
                pointConfig.get("blockSize").getValue(Integer.class),
                type);
    }

    /**
     * 向 PLC S7 写入数据
     * <p>
     * 该方法用于将指定类型的数据写入到 PLC S7 的指定点位。
     * 1. 根据类型字符串获取对应的 {@link AttributeTypeFlagEnum} 枚举值。
     * 2. 如果类型不支持, 抛出 {@link UnSupportException} 异常。
     * 3. 根据类型将字符串值转换为相应的 Java 类型。
     * 4. 使用 {@link S7Serializer} 将数据写入到 PLC S7 的指定数据块和字节偏移量位置。
     * <p>
     * 支持的数据类型包括：
     * - INT: 整型
     * - LONG: 长整型
     * - FLOAT: 单精度浮点型
     * - DOUBLE: 双精度浮点型
     * - BOOLEAN: 布尔型
     * - STRING: 字符串
     *
     * @param serializer         S7 序列化器, 用于与 PLC S7 进行数据交互
     * @param plcS7PointVariable PLC S7 点位变量信息, 包含数据块编号, 字节偏移量等
     * @param type               数据类型字符串, 用于标识要写入的数据类型
     * @param value              要写入的字符串形式的数据值
     * @throws UnSupportException 如果数据类型不支持, 抛出此异常
     */
    private void store(S7Serializer serializer, PlcS7PointVariable plcS7PointVariable, String type, String value) {
        AttributeTypeFlagEnum valueType = AttributeTypeFlagEnum.ofCode(type);
        if (Objects.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of " + type);
        }
        AttributeBO attributeBOConfig = new AttributeBO(value, valueType);

        switch (valueType) {
            case INT:
                int intValue = attributeBOConfig.getValue(Integer.class);
                serializer.store(intValue, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            case LONG:
                long longValue = attributeBOConfig.getValue(Long.class);
                serializer.store(longValue, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            case FLOAT:
                float floatValue = attributeBOConfig.getValue(Float.class);
                serializer.store(floatValue, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            case DOUBLE:
                double doubleValue = attributeBOConfig.getValue(Double.class);
                serializer.store(doubleValue, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            case BOOLEAN:
                boolean booleanValue = attributeBOConfig.getValue(Boolean.class);
                serializer.store(booleanValue, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            case STRING:
                serializer.store(value, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            default:
                break;
        }
    }

    /**
     * MyS7Connector 内部类
     * <p>
     * 该类用于封装与 PLC S7 连接相关的信息, 包括读写锁和 S7 连接器。
     * 读写锁 {@link ReentrantReadWriteLock} 用于确保在多线程环境下对 S7 连接器的操作是线程安全的。
     * S7 连接器 {@link S7Connector} 用于与 PLC S7 设备进行通信。
     * <p>
     * 该类提供了无参构造函数和全参构造函数, 并使用了 Lombok 注解自动生成 getter 和 setter 方法。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class MyS7Connector {
        private ReentrantReadWriteLock lock;
        private S7Connector connector;
    }

}
