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
import io.github.pnoker.common.driver.entity.bo.MetadataEventBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.base.BaseBO;
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
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    DriverMetadata driverMetadata;

    @Resource
    private DriverSenderService driverSenderService;

    static ModbusFactory modbusFactory;

    static {
        modbusFactory = new ModbusFactory();
    }

    private Map<Long, ModbusMaster> connectMap;

    @Override
    public void initial() {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        你可以在此处执行一些特定的初始化逻辑, 驱动在启动的时候会自动执行该方法。
        */
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        上传设备状态, 可自行灵活拓展, 不一定非要在schedule()接口中实现, 你可以: 
        - 在read中实现设备状态的判断;
        - 在自定义定时任务中实现设备状态的判断;
        - 根据某种判断机制实现设备状态的判断。

        最后根据 driverSenderService.deviceStatusSender(deviceId,deviceStatus) 接口将设备状态交给SDK管理, 其中设备状态(StatusEnum):
        - ONLINE:在线
        - OFFLINE:离线
        - MAINTAIN:维护
        - FAULT:故障
         */
        driverMetadata.getDeviceIds().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventBO<? extends BaseBO> metadataEvent) {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        接收驱动, 设备, 位号元数据新增, 更新, 删除都会触发改事件
        提供元数据类型: MetadataTypeEnum(DRIVER, DEVICE, POINT)
        提供元数据操作类型: MetadataOperateTypeEnum(ADD, DELETE, UPDATE)
         */
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            // to do something for device event
            DeviceBO metadata = (DeviceBO) metadataEvent.getMetadata();
            log.info("Device metadata event: deviceId: {}, operate: {}, metadata: {}", metadata.getId(), operateType, metadata);

            // 当设备更新或者删除时，移除连接句柄
            if (MetadataOperateTypeEnum.DELETE.equals(operateType) || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                connectMap.remove(metadata.getId());
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            // to do something for point event
            PointBO metadata = (PointBO) metadataEvent.getMetadata();
            log.info("Point metadata event: pointId: {}, operate: {}, metadata: {}", metadata.getId(), operateType, metadata);
        }
    }

    @Override
    public RValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point) {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        */
        return new RValue(device, point, readValue(getConnector(device.getId(), driverConfig), pointConfig, point.getPointTypeFlag().getCode()));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        */
        ModbusMaster modbusMaster = getConnector(device.getId(), driverConfig);
        return writeValue(modbusMaster, pointConfig, wValue);
    }

    /**
     * 获取 Modbus Master
     *
     * @param deviceId     设备ID
     * @param driverConfig 驱动信息
     * @return ModbusMaster
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
     * 获取 Value
     *
     * @param modbusMaster ModbusMaster
     * @param pointConfig  位号属性配置
     * @param type         Value Type
     * @return R of String Value
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
     * 获取 ModbusMaster 值
     *
     * @param modbusMaster ModbusMaster
     * @param locator      BaseLocator
     * @param <T>          类型
     * @return 类型
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
     * 写入 ModbusMaster 值
     *
     * @param modbusMaster ModbusMaster
     * @param pointConfig  位号属性配置
     * @param wValue       Value
     * @return Write Result
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
     * 获取数据类型
     * 说明: 此处可根据实际项目情况进行拓展
     * 1.swap 交换
     * 2.大端/小端,默认是大端
     * 3.拓展其他数据类型
     *
     * @param type Value Type
     * @return Modbus Data Type
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
     * 写入 ModbusMaster 值
     *
     * @param modbusMaster ModbusMaster
     * @param slaveId      从站ID
     * @param offset       偏移量
     * @param wValue       写入值
     * @return WriteCoilResponse
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
     * 写入 ModbusMaster 值
     *
     * @param modbusMaster ModbusMaster
     * @param locator      BaseLocator
     * @param wValue       写入值
     * @param <T>          类型
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
