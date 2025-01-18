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

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.serotonin.modbus4j.msg.WriteCoilRequest;
import com.serotonin.modbus4j.msg.WriteCoilResponse;
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
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.exception.WritePointException;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * 驱动自定义服务实现类
 *
 * @author pnoker
 * @version 2024.3.10
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    static ModbusFactory modbusFactory;

    static {
        modbusFactory = new ModbusFactory();
    }

    @Resource
    DriverMetadata driverMetadata;
    @Resource
    private DriverSenderService driverSenderService;
    private Map<Long, ModbusMaster> connectMap;

    @Override
    public void initial() {
        /*
         * 驱动初始化逻辑
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 驱动启动时会自动执行该方法，您可以在此处执行特定的初始化操作。
         *
         */
        connectMap = new ConcurrentHashMap<>(16);
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
         * 读取设备点位数据
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 通过 Modbus 连接器读取指定设备的点位数据，并返回 RValue 对象。
         * RValue 对象包含设备信息、点位信息以及读取到的值。
         */
        return new RValue(device, point, readValue(getConnector(device.getId(), driverConfig), pointConfig, point.getPointTypeFlag().getCode()));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
         * 写入设备点位数据
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 通过 Modbus 连接器将指定值写入设备的点位，并返回写入结果。
         */
        ModbusMaster modbusMaster = getConnector(device.getId(), driverConfig);
        return writeValue(modbusMaster, pointConfig, wValue);
    }

    /**
     * 获取 Modbus Master 连接器
     * <p>
     * 该方法用于根据设备ID和驱动配置获取或创建 Modbus Master 连接器。
     * 如果连接器已存在，则直接返回；否则，根据配置创建新的连接器并初始化。
     * 初始化失败时，会移除连接器并抛出异常。
     *
     * @param deviceId     设备ID，用于标识唯一的设备连接
     * @param driverConfig 驱动配置，包含连接 Modbus 设备所需的主机地址和端口号
     * @return ModbusMaster 返回与设备关联的 Modbus Master 连接器
     * @throws ConnectorException 如果连接器初始化失败，抛出此异常
     */
    private ModbusMaster getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        log.debug("Modbus Tcp Connection Info: {}", JsonUtil.toJsonString(driverConfig));
        ModbusMaster modbusMaster = connectMap.get(deviceId);
        if (Objects.isNull(modbusMaster)) {
            IpParameters params = new IpParameters();
            params.setHost(driverConfig.get("host").getValue(String.class));
            params.setPort(driverConfig.get("port").getValue(Integer.class));
            modbusMaster = modbusFactory.createTcpMaster(params, true);
            try {
                modbusMaster.init();
                connectMap.put(deviceId, modbusMaster);
            } catch (ModbusInitException e) {
                connectMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
                log.error("Connect modbus master error: {}", e.getMessage(), e);
                throw new ConnectorException(e.getMessage());
            }
        }
        return modbusMaster;
    }

    /**
     * 读取 Modbus 设备点位值
     * <p>
     * 根据点位配置中的功能码（functionCode）和偏移量（offset），从 Modbus 设备中读取相应类型的值。
     * 支持的功能码包括：
     * - 1: 读取线圈状态（Coil Status）
     * - 2: 读取输入状态（Input Status）
     * - 3: 读取保持寄存器（Holding Register）
     * - 4: 读取输入寄存器（Input Register）
     *
     * @param modbusMaster ModbusMaster 连接器，用于与设备通信
     * @param pointConfig  点位配置，包含从站ID（slaveId）、功能码（functionCode）、偏移量（offset）等信息
     * @param type         点位值类型，用于确定寄存器中数据的解析方式
     * @return String 返回读取到的点位值，以字符串形式表示。如果功能码不支持，则返回 "0"。
     */
    private String readValue(ModbusMaster modbusMaster, Map<String, AttributeBO> pointConfig, String type) {
        int slaveId = pointConfig.get("slaveId").getValue(Integer.class);
        int functionCode = pointConfig.get("functionCode").getValue(Integer.class);
        int offset = pointConfig.get("offset").getValue(Integer.class);
        switch (functionCode) {
            case 1:
                BaseLocator<Boolean> coilLocator = BaseLocator.coilStatus(slaveId, offset);
                Boolean coilValue = getMasterValue(modbusMaster, coilLocator);
                return String.valueOf(coilValue);
            case 2:
                BaseLocator<Boolean> inputLocator = BaseLocator.inputStatus(slaveId, offset);
                Boolean inputStatusValue = getMasterValue(modbusMaster, inputLocator);
                return String.valueOf(inputStatusValue);
            case 3:
                BaseLocator<Number> holdingLocator = BaseLocator.holdingRegister(slaveId, offset, getValueType(type));
                Number holdingValue = getMasterValue(modbusMaster, holdingLocator);
                return String.valueOf(holdingValue);
            case 4:
                BaseLocator<Number> inputRegister = BaseLocator.inputRegister(slaveId, offset, getValueType(type));
                Number inputRegisterValue = getMasterValue(modbusMaster, inputRegister);
                return String.valueOf(inputRegisterValue);
            default:
                return "0";
        }
    }

    /**
     * 从 ModbusMaster 连接器中读取指定点位的数据
     * <p>
     * 该方法通过给定的 {@link BaseLocator} 从 ModbusMaster 连接器中读取数据。
     * 如果读取过程中发生 {@link ModbusTransportException} 或 {@link ErrorResponseException} 异常，
     * 将记录错误日志并抛出 {@link ReadPointException} 异常。
     *
     * @param modbusMaster ModbusMaster 连接器，用于与设备通信
     * @param locator      点位定位器，包含从站ID、功能码、偏移量等信息
     * @param <T>          返回值类型，根据点位的数据类型确定
     * @return T 返回读取到的点位数据
     * @throws ReadPointException 如果读取过程中发生异常，抛出此异常
     */
    private <T> T getMasterValue(ModbusMaster modbusMaster, BaseLocator<T> locator) {
        try {
            return modbusMaster.getValue(locator);
        } catch (ModbusTransportException | ErrorResponseException e) {
            log.error("Read modbus master value error: {}", e.getMessage(), e);
            throw new ReadPointException(e.getMessage());
        }
    }

    /**
     * 向 Modbus 设备写入点位值
     * <p>
     * 根据点位配置中的功能码（functionCode）和偏移量（offset），将指定值写入 Modbus 设备的相应点位。
     * 支持的功能码包括：
     * - 1: 写入线圈状态（Coil Status）
     * - 3: 写入保持寄存器（Holding Register）
     * <p>
     * 对于功能码 1，写入布尔值到线圈状态，并返回写入结果。
     * 对于功能码 3，写入数值到保持寄存器，并返回写入成功状态。
     * 其他功能码暂不支持，返回 false。
     *
     * @param modbusMaster ModbusMaster 连接器，用于与设备通信
     * @param pointConfig  点位配置，包含从站ID（slaveId）、功能码（functionCode）、偏移量（offset）等信息
     * @param wValue       待写入的值，包含值类型和具体数值
     * @return boolean 返回写入结果，true 表示写入成功，false 表示写入失败或不支持的功能码
     */
    private boolean writeValue(ModbusMaster modbusMaster, Map<String, AttributeBO> pointConfig, WValue wValue) {
        int slaveId = pointConfig.get("slaveId").getValue(Integer.class);
        int functionCode = pointConfig.get("functionCode").getValue(Integer.class);
        int offset = pointConfig.get("offset").getValue(Integer.class);
        switch (functionCode) {
            case 1:
                WriteCoilResponse coilResponse = setMasterValue(modbusMaster, slaveId, offset, wValue);
                return !coilResponse.isException();
            case 3:
                BaseLocator<Number> locator = BaseLocator.holdingRegister(slaveId, offset, getValueType(wValue.getType().getCode()));
                setMasterValue(modbusMaster, locator, wValue);
                return true;
            default:
                return false;
        }
    }

    /**
     * 获取 Modbus 数据类型
     * <p>
     * 根据点位值类型（type）返回对应的 Modbus 数据类型。
     * 支持的点位值类型包括：
     * - {@link PointTypeFlagEnum#LONG}: 返回 4 字节有符号整数（{@link DataType#FOUR_BYTE_INT_SIGNED}）
     * - {@link PointTypeFlagEnum#FLOAT}: 返回 4 字节浮点数（{@link DataType#FOUR_BYTE_FLOAT}）
     * - {@link PointTypeFlagEnum#DOUBLE}: 返回 8 字节浮点数（{@link DataType#EIGHT_BYTE_FLOAT}）
     * - 其他类型: 返回 2 字节有符号整数（{@link DataType#TWO_BYTE_INT_SIGNED}）
     * <p>
     * 提示: 该方法可根据实际项目需求进行扩展，例如支持字节交换、大端/小端模式等。
     *
     * @param type 点位值类型，用于确定 Modbus 数据类型
     * @return int 返回对应的 Modbus 数据类型
     * @throws UnSupportException 如果点位值类型不支持，抛出此异常
     */
    private int getValueType(String type) {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(type);
        if (Objects.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of " + type);
        }

        switch (valueType) {
            case LONG:
                return DataType.FOUR_BYTE_INT_SIGNED;
            case FLOAT:
                return DataType.FOUR_BYTE_FLOAT;
            case DOUBLE:
                return DataType.EIGHT_BYTE_FLOAT;
            default:
                return DataType.TWO_BYTE_INT_SIGNED;
        }
    }

    /**
     * 向 Modbus 设备写入线圈状态值
     * <p>
     * 该方法通过 ModbusMaster 连接器向指定从站（slaveId）的线圈（offset）写入布尔值。
     * 如果写入过程中发生 {@link ModbusTransportException} 异常，将记录错误日志并抛出 {@link WritePointException} 异常。
     *
     * @param modbusMaster ModbusMaster 连接器，用于与设备通信
     * @param slaveId      从站ID，标识目标设备
     * @param offset       线圈偏移量，标识目标线圈
     * @param wValue       待写入的值，包含布尔值
     * @return WriteCoilResponse 返回写入操作的响应结果
     * @throws WritePointException 如果写入过程中发生异常，抛出此异常
     */
    private WriteCoilResponse setMasterValue(ModbusMaster modbusMaster, int slaveId, int offset, WValue wValue) {
        try {
            WriteCoilRequest coilRequest = new WriteCoilRequest(slaveId, offset, wValue.getValue(Boolean.class));
            return (WriteCoilResponse) modbusMaster.send(coilRequest);
        } catch (ModbusTransportException e) {
            log.error("Write modbus master value error: {}", e.getMessage(), e);
            throw new WritePointException(e.getMessage());
        }
    }

    /**
     * 向 Modbus 设备写入指定类型的值
     * <p>
     * 该方法通过 ModbusMaster 连接器向指定点位写入值。点位信息由 {@link BaseLocator} 定义，
     * 写入的值类型由 {@link WValue} 指定。支持写入浮点数类型的数据。
     * <p>
     * 如果写入过程中发生 {@link ModbusTransportException} 或 {@link ErrorResponseException} 异常，
     * 将记录错误日志并抛出 {@link WritePointException} 异常。
     *
     * @param modbusMaster ModbusMaster 连接器，用于与设备通信
     * @param locator      点位定位器，包含从站ID、功能码、偏移量等信息
     * @param wValue       待写入的值，包含值类型和具体数值
     * @param <T>          返回值类型，根据点位的数据类型确定
     * @throws WritePointException 如果写入过程中发生异常，抛出此异常
     */
    private <T> void setMasterValue(ModbusMaster modbusMaster, BaseLocator<T> locator, WValue wValue) {
        try {
            modbusMaster.setValue(locator, wValue.getValue(Float.class));
        } catch (ModbusTransportException | ErrorResponseException e) {
            log.error("Write modbus master value error: {}", e.getMessage(), e);
            throw new WritePointException(e.getMessage());
        }
    }

}
