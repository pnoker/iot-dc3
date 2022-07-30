/*
 * Copyright 2022 Pnoker All Rights Reserved
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

import io.github.pnoker.common.bean.driver.AttributeInfo;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.constant.ValueConstant;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.sdk.bean.driver.DriverContext;
import io.github.pnoker.common.sdk.service.DriverCustomService;
import io.github.pnoker.common.sdk.service.DriverService;
import io.github.pnoker.common.utils.JsonUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static io.github.pnoker.common.sdk.utils.DriverUtil.attribute;
import static io.github.pnoker.common.sdk.utils.DriverUtil.value;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;

    static ModbusFactory modbusFactory;

    static {
        modbusFactory = new ModbusFactory();
    }

    private volatile Map<String, ModbusMaster> masterMap = new HashMap<>(64);

    @Override
    public void initial() {
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) throws Exception {
        ModbusMaster modbusMaster = getMaster(device.getId(), driverInfo);
        return readValue(modbusMaster, pointInfo, point.getType());
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) throws Exception {
        ModbusMaster modbusMaster = getMaster(device.getId(), driverInfo);
        return writeValue(modbusMaster, pointInfo, value.getType(), value.getValue());
    }

    @Override
    public void schedule() {

        /*
        TODO:设备状态
        上传设备状态，可自行灵活拓展，不一定非要在schedule()接口中实现，也可以在read中实现设备状态的设置；
        你可以通过某种判断机制确定设备的状态，然后通过driverService.deviceEventSender接口将设备状态交给SDK管理。

        设备状态（DeviceStatus）如下：
        ONLINE:在线
        OFFLINE:离线
        MAINTAIN:维护
        FAULT:故障
         */
        driverContext.getDriverMetadata().getDeviceMap().keySet().forEach(id -> driverService.deviceEventSender(id, CommonConstant.Device.Event.HEARTBEAT, CommonConstant.Status.ONLINE));
    }

    /**
     * 获取 Modbus Master
     *
     * @param deviceId   Device Id
     * @param driverInfo Driver Info
     * @return ModbusMaster
     * @throws ModbusInitException ModbusInitException
     */
    public ModbusMaster getMaster(String deviceId, Map<String, AttributeInfo> driverInfo) throws ModbusInitException {
        log.debug("Modbus Tcp Connection Info {}", JsonUtil.toJsonString(driverInfo));
        ModbusMaster modbusMaster = masterMap.get(deviceId);
        if (null == modbusMaster) {
            IpParameters params = new IpParameters();
            params.setHost(attribute(driverInfo, "host"));
            params.setPort(attribute(driverInfo, "port"));
            modbusMaster = modbusFactory.createTcpMaster(params, true);
            modbusMaster.init();
            masterMap.put(deviceId, modbusMaster);
        }
        return modbusMaster;
    }

    /**
     * 获取 Value
     *
     * @param modbusMaster ModbusMaster
     * @param pointInfo    Point Info
     * @return String Value
     * @throws ModbusTransportException ModbusTransportException
     * @throws ErrorResponseException   ErrorResponseException
     */
    public String readValue(ModbusMaster modbusMaster, Map<String, AttributeInfo> pointInfo, String type) throws ModbusTransportException, ErrorResponseException {
        int slaveId = attribute(pointInfo, "slaveId");
        int functionCode = attribute(pointInfo, "functionCode");
        int offset = attribute(pointInfo, "offset");
        switch (functionCode) {
            case 1:
                BaseLocator<Boolean> coilLocator = BaseLocator.coilStatus(slaveId, offset);
                Boolean coilValue = modbusMaster.getValue(coilLocator);
                return String.valueOf(coilValue);
            case 2:
                BaseLocator<Boolean> inputLocator = BaseLocator.inputStatus(slaveId, offset);
                Boolean inputStatusValue = modbusMaster.getValue(inputLocator);
                return String.valueOf(inputStatusValue);
            case 3:
                BaseLocator<Number> holdingLocator = BaseLocator.holdingRegister(slaveId, offset, getValueType(type));
                Number holdingValue = modbusMaster.getValue(holdingLocator);
                return String.valueOf(holdingValue);
            case 4:
                BaseLocator<Number> inputRegister = BaseLocator.inputRegister(slaveId, offset, getValueType(type));
                Number inputRegisterValue = modbusMaster.getValue(inputRegister);
                return String.valueOf(inputRegisterValue);
            default:
                return "0";
        }
    }

    /**
     * 写 Value
     *
     * @param modbusMaster ModbusMaster
     * @param pointInfo    Point Info
     * @param type         Value Type
     * @param value        String Value
     * @return Write Result
     * @throws ModbusTransportException ModbusTransportException
     * @throws ErrorResponseException   ErrorResponseException
     */
    public boolean writeValue(ModbusMaster modbusMaster, Map<String, AttributeInfo> pointInfo, String type, String value) throws ModbusTransportException, ErrorResponseException {
        int slaveId = attribute(pointInfo, "slaveId");
        int functionCode = attribute(pointInfo, "functionCode");
        int offset = attribute(pointInfo, "offset");
        switch (functionCode) {
            case 1:
                boolean coilValue = value(type, value);
                WriteCoilRequest coilRequest = new WriteCoilRequest(slaveId, offset, coilValue);
                WriteCoilResponse coilResponse = (WriteCoilResponse) modbusMaster.send(coilRequest);
                return !coilResponse.isException();
            case 3:
                BaseLocator<Number> locator = BaseLocator.holdingRegister(slaveId, offset, getValueType(type));
                modbusMaster.setValue(locator, value(type, value));
                return true;
            default:
                return false;
        }
    }

    /**
     * 获取数据类型
     * 说明：此处可根据实际项目情况进行拓展
     * 1.swap 交换
     * 2.大端/小端,默认是大端
     * 3.拓展其他数据类型
     *
     * @param type Value Type
     * @return Modbus Data Type
     */
    public int getValueType(String type) {
        switch (type.toLowerCase()) {
            case ValueConstant.Type.LONG:
                return DataType.FOUR_BYTE_INT_SIGNED;
            case ValueConstant.Type.FLOAT:
                return DataType.FOUR_BYTE_FLOAT;
            case ValueConstant.Type.DOUBLE:
                return DataType.EIGHT_BYTE_FLOAT;
            default:
                return DataType.TWO_BYTE_INT_SIGNED;
        }
    }

}
