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
package com.serotonin.modbus4j.msg;

import com.serotonin.modbus4j.Modbus;
import com.serotonin.modbus4j.ProcessImage;
import com.serotonin.modbus4j.code.FunctionCode;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>ReportSlaveIdRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class ReportSlaveIdRequest extends ModbusRequest {
    /**
     * <p>Constructor for ReportSlaveIdRequest.</p>
     *
     * @param slaveId a int.
     * @throws ModbusTransportException if any.
     */
    public ReportSlaveIdRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Modbus modbus) {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRequest(ByteQueue queue) {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readRequest(ByteQueue queue) {
        // no op
    }

    @Override
    ModbusResponse getResponseInstance(int slaveId) throws ModbusTransportException {
        return new ReportSlaveIdResponse(slaveId);
    }

    @Override
    ModbusResponse handleImpl(ProcessImage processImage) throws ModbusTransportException {
        return new ReportSlaveIdResponse(slaveId, processImage.getReportSlaveIdData());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return FunctionCode.REPORT_SLAVE_ID;
    }
}
