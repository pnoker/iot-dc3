/*
 * Copyright 2016-present the original author or authors.
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
package com.serotonin.modbus4j;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.exception.ModbusIdException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.ip.listener.TcpListener;
import com.serotonin.modbus4j.ip.tcp.TcpMaster;
import com.serotonin.modbus4j.ip.tcp.TcpSlave;
import com.serotonin.modbus4j.ip.udp.UdpMaster;
import com.serotonin.modbus4j.ip.udp.UdpSlave;
import com.serotonin.modbus4j.msg.*;
import com.serotonin.modbus4j.serial.SerialPortWrapper;
import com.serotonin.modbus4j.serial.ascii.AsciiMaster;
import com.serotonin.modbus4j.serial.ascii.AsciiSlave;
import com.serotonin.modbus4j.serial.rtu.RtuMaster;
import com.serotonin.modbus4j.serial.rtu.RtuSlave;

/**
 * <p>ModbusFactory class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class ModbusFactory {
    //
    // Modbus masters
    //

    /**
     * <p>createRtuMaster.</p>
     *
     * @param wrapper a {@link SerialPortWrapper} object.
     * @return a {@link ModbusMaster} object.
     */
    public ModbusMaster createRtuMaster(SerialPortWrapper wrapper) {
        return new RtuMaster(wrapper);
    }

    /**
     * <p>createAsciiMaster.</p>
     *
     * @param wrapper a {@link SerialPortWrapper} object.
     * @return a {@link ModbusMaster} object.
     */
    public ModbusMaster createAsciiMaster(SerialPortWrapper wrapper) {
        return new AsciiMaster(wrapper);
    }

    /**
     * <p>createTcpMaster.</p>
     *
     * @param params    a {@link IpParameters} object.
     * @param keepAlive a boolean.
     * @return a {@link ModbusMaster} object.
     */
    public ModbusMaster createTcpMaster(IpParameters params, boolean keepAlive) {
        return new TcpMaster(params, keepAlive);
    }

    /**
     * <p>createTcpMaster.</p>
     *
     * @param params     a {@link IpParameters} object.
     * @param keepAlive  a boolean.
     * @param lingerTime an Integer.
     * @return a {@link ModbusMaster} object.
     */
    public ModbusMaster createTcpMaster(IpParameters params, boolean keepAlive, Integer lingerTime) {
        return new TcpMaster(params, keepAlive, lingerTime);
    }

    /**
     * <p>createUdpMaster.</p>
     *
     * @param params a {@link IpParameters} object.
     * @return a {@link ModbusMaster} object.
     */
    public ModbusMaster createUdpMaster(IpParameters params) {
        return new UdpMaster(params);
    }

    /**
     * <p>createTcpListener.</p>
     *
     * @param params a {@link IpParameters} object.
     * @return a {@link ModbusMaster} object.
     */
    public ModbusMaster createTcpListener(IpParameters params) {
        return new TcpListener(params);
    }

    //
    // Modbus slaves
    //

    /**
     * <p>createRtuSlave.</p>
     *
     * @param wrapper a {@link SerialPortWrapper} object.
     * @return a {@link ModbusSlaveSet} object.
     */
    public ModbusSlaveSet createRtuSlave(SerialPortWrapper wrapper) {
        return new RtuSlave(wrapper);
    }

    /**
     * <p>createAsciiSlave.</p>
     *
     * @param wrapper a {@link SerialPortWrapper} object.
     * @return a {@link ModbusSlaveSet} object.
     */
    public ModbusSlaveSet createAsciiSlave(SerialPortWrapper wrapper) {
        return new AsciiSlave(wrapper);
    }

    /**
     * <p>createTcpSlave.</p>
     *
     * @param encapsulated a boolean.
     * @return a {@link ModbusSlaveSet} object.
     */
    public ModbusSlaveSet createTcpSlave(boolean encapsulated) {
        return new TcpSlave(encapsulated);
    }

    /**
     * <p>createUdpSlave.</p>
     *
     * @param encapsulated a boolean.
     * @return a {@link ModbusSlaveSet} object.
     */
    public ModbusSlaveSet createUdpSlave(boolean encapsulated) {
        return new UdpSlave(encapsulated);
    }

    //
    // Modbus requests
    //

    /**
     * <p>createReadRequest.</p>
     *
     * @param slaveId a int.
     * @param range   a int.
     * @param offset  a int.
     * @param length  a int.
     * @return a {@link ModbusRequest} object.
     * @throws ModbusTransportException if any.
     * @throws ModbusIdException        if any.
     */
    public ModbusRequest createReadRequest(int slaveId, int range, int offset, int length)
            throws ModbusTransportException, ModbusIdException {
        ModbusUtils.validateRegisterRange(range);

        if (range == RegisterRange.COIL_STATUS)
            return new ReadCoilsRequest(slaveId, offset, length);

        if (range == RegisterRange.INPUT_STATUS)
            return new ReadDiscreteInputsRequest(slaveId, offset, length);

        if (range == RegisterRange.INPUT_REGISTER)
            return new ReadInputRegistersRequest(slaveId, offset, length);

        return new ReadHoldingRegistersRequest(slaveId, offset, length);
    }
}
