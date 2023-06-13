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

import com.serotonin.modbus4j.base.KeyedModbusLocator;
import com.serotonin.modbus4j.base.ReadFunctionGroup;
import com.serotonin.modbus4j.base.SlaveProfile;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.code.ExceptionCode;
import com.serotonin.modbus4j.code.FunctionCode;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.InvalidDataConversionException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.serotonin.modbus4j.locator.BinaryLocator;
import com.serotonin.modbus4j.locator.NumericLocator;
import com.serotonin.modbus4j.msg.*;
import com.serotonin.modbus4j.sero.epoll.InputStreamEPollWrapper;
import com.serotonin.modbus4j.sero.log.BaseIOLog;
import com.serotonin.modbus4j.sero.messaging.MessageControl;
import com.serotonin.modbus4j.sero.util.ArrayUtils;
import com.serotonin.modbus4j.sero.util.ProgressiveTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Abstract ModbusMaster class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
abstract public class ModbusMaster extends Modbus {
    private int timeout = 500;
    private int retries = 2;

    /**
     * Should we validate the responses:
     * - ensure that the requested slave id is what is in the response
     */
    protected boolean validateResponse;

    /**
     * If connection is established with slave/slaves
     */
    protected boolean connected = false;

    /**
     * <p>isConnected.</p>
     *
     * @return a boolean.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * <p>Setter for the field <code>connected</code>.</p>
     *
     * @param connected a boolean.
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    /**
     * If the slave equipment only supports multiple write commands, set this to true. Otherwise, and combination of
     * single or multiple write commands will be used as appropriate.
     */
    private boolean multipleWritesOnly;

    private int discardDataDelay = 0;
    private BaseIOLog ioLog;

    /**
     * An input stream ePoll will use a single thread to read all input streams. If multiple serial or TCP modbus
     * connections are to be made, an ePoll can be much more efficient.
     */
    private InputStreamEPollWrapper ePoll;

    private final Map<Integer, SlaveProfile> slaveProfiles = new HashMap<>();
    protected boolean initialized;

    /**
     * <p>init.</p>
     *
     * @throws ModbusInitException if any.
     */
    abstract public void init() throws ModbusInitException;

    /**
     * <p>isInitialized.</p>
     *
     * @return a boolean.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * <p>destroy.</p>
     */
    abstract public void destroy();

    /**
     * <p>send.</p>
     *
     * @param request a {@link ModbusRequest} object.
     * @return a {@link ModbusResponse} object.
     * @throws ModbusTransportException if any.
     */
    public final ModbusResponse send(ModbusRequest request) throws ModbusTransportException {
        request.validate(this);
        ModbusResponse modbusResponse = sendImpl(request);
        if (validateResponse)
            modbusResponse.validateResponse(request);
        return modbusResponse;
    }

    /**
     * <p>sendImpl.</p>
     *
     * @param request a {@link ModbusRequest} object.
     * @return a {@link ModbusResponse} object.
     * @throws ModbusTransportException if any.
     */
    abstract public ModbusResponse sendImpl(ModbusRequest request) throws ModbusTransportException;

    /**
     * Returns a value from the modbus network according to the given locator information. Various data types are
     * allowed to be requested including multi-word types. The determination of the correct request message to send is
     * handled automatically.
     *
     * @param locator the information required to locate the value in the modbus network.
     * @param <T>     a T object.
     * @return an object representing the value found. This will be one of Boolean, Short, Integer, Long, BigInteger,
     * Float, or Double. See the DataType enumeration for details on which type to expect.
     * @throws ModbusTransportException if there was an IO error or other technical failure while sending the message
     * @throws ErrorResponseException   if the response returned from the slave was an exception.
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(BaseLocator<T> locator) throws ModbusTransportException, ErrorResponseException {
        BatchRead<String> batch = new BatchRead<>();
        batch.addLocator("", locator);
        BatchResults<String> result = send(batch);
        return (T) result.getValue("");
    }

    /**
     * Sets the given value in the modbus network according to the given locator information. Various data types are
     * allowed to be set including including multi-word types. The determination of the correct write message to send is
     * handled automatically.
     *
     * @param locator the information required to locate the value in the modbus network.
     * @param value   an object representing the value to be set. This will be one of Boolean, Short, Integer, Long, BigInteger,
     *                Float, or Double. See the DataType enumeration for details on which type to expect.
     * @param <T>     type of locator
     * @throws ModbusTransportException if there was an IO error or other technical failure while sending the message
     * @throws ErrorResponseException   if the response returned from the slave was an exception.
     */
    public <T> void setValue(BaseLocator<T> locator, Object value) throws ModbusTransportException,
            ErrorResponseException {
        int slaveId = locator.getSlaveId();
        int registerRange = locator.getRange();
        int writeOffset = locator.getOffset();

        // Determine the request type that we will use
        if (registerRange == RegisterRange.INPUT_STATUS || registerRange == RegisterRange.INPUT_REGISTER)
            throw new RuntimeException("Cannot write to input status or input register ranges");

        if (registerRange == RegisterRange.COIL_STATUS) {
            if (!(value instanceof Boolean))
                throw new InvalidDataConversionException("Only boolean values can be written to coils");
            if (multipleWritesOnly)
                setValue(new WriteCoilsRequest(slaveId, writeOffset, new boolean[]{((Boolean) value).booleanValue()}));
            else
                setValue(new WriteCoilRequest(slaveId, writeOffset, ((Boolean) value).booleanValue()));
        } else {
            // Writing to holding registers.
            if (locator.getDataType() == DataType.BINARY) {
                if (!(value instanceof Boolean))
                    throw new InvalidDataConversionException("Only boolean values can be written to coils");
                setHoldingRegisterBit(slaveId, writeOffset, ((BinaryLocator) locator).getBit(),
                        ((Boolean) value).booleanValue());
            } else {
                // Writing some kind of value to a holding register.
                @SuppressWarnings("unchecked")
                short[] data = locator.valueToShorts((T) value);
                if (data.length == 1 && !multipleWritesOnly)
                    setValue(new WriteRegisterRequest(slaveId, writeOffset, data[0]));
                else
                    setValue(new WriteRegistersRequest(slaveId, writeOffset, data));
            }
        }

    }

    /**
     * Node scanning. Returns a list of slave nodes that respond to a read exception status request (perhaps with an
     * error, but respond nonetheless).
     * <p>
     * Note: a similar scan could be done for registers in nodes, but, for one thing, it would take some time to run,
     * and in any case the results would not be meaningful since there would be no semantic information accompanying the
     * results.
     *
     * @return a {@link List} object.
     */
    public List<Integer> scanForSlaveNodes() {
        List<Integer> result = new ArrayList<>();
        for (int i = 1; i <= 240; i++) {
            if (testSlaveNode(i))
                result.add(i);
        }
        return result;
    }

    /**
     * <p>scanForSlaveNodes.</p>
     *
     * @param l a {@link NodeScanListener} object.
     * @return a {@link ProgressiveTask} object.
     */
    public ProgressiveTask scanForSlaveNodes(final NodeScanListener l) {
        l.progressUpdate(0);
        ProgressiveTask task = new ProgressiveTask(l) {
            private int node = 1;

            @Override
            protected void runImpl() {
                if (testSlaveNode(node))
                    l.nodeFound(node);

                declareProgress(((float) node) / 240);

                node++;
                if (node > 240)
                    completed = true;
            }
        };

        new Thread(task).start();

        return task;
    }

    /**
     * <p>testSlaveNode.</p>
     *
     * @param node a int.
     * @return a boolean.
     */
    public boolean testSlaveNode(int node) {
        try {
            send(new ReadHoldingRegistersRequest(node, 0, 1));
        } catch (ModbusTransportException e) {
            // If there was a transport exception, there's no node there.
            return false;
        }
        return true;
    }

    /**
     * <p>Getter for the field <code>retries</code>.</p>
     *
     * @return a int.
     */
    public int getRetries() {
        return retries;
    }

    /**
     * <p>Setter for the field <code>retries</code>.</p>
     *
     * @param retries a int.
     */
    public void setRetries(int retries) {
        if (retries < 0)
            this.retries = 0;
        else
            this.retries = retries;
    }

    /**
     * <p>Getter for the field <code>timeout</code>.</p>
     *
     * @return a int.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * <p>Setter for the field <code>timeout</code>.</p>
     *
     * @param timeout a int.
     */
    public void setTimeout(int timeout) {
        if (timeout < 1)
            this.timeout = 1;
        else
            this.timeout = timeout;
    }

    /**
     * <p>isMultipleWritesOnly.</p>
     *
     * @return a boolean.
     */
    public boolean isMultipleWritesOnly() {
        return multipleWritesOnly;
    }

    /**
     * <p>Setter for the field <code>multipleWritesOnly</code>.</p>
     *
     * @param multipleWritesOnly a boolean.
     */
    public void setMultipleWritesOnly(boolean multipleWritesOnly) {
        this.multipleWritesOnly = multipleWritesOnly;
    }

    /**
     * <p>Getter for the field <code>discardDataDelay</code>.</p>
     *
     * @return a int.
     */
    public int getDiscardDataDelay() {
        return discardDataDelay;
    }

    /**
     * <p>Setter for the field <code>discardDataDelay</code>.</p>
     *
     * @param discardDataDelay a int.
     */
    public void setDiscardDataDelay(int discardDataDelay) {
        if (discardDataDelay < 0)
            this.discardDataDelay = 0;
        else
            this.discardDataDelay = discardDataDelay;
    }

    /**
     * <p>Getter for the field <code>ioLog</code>.</p>
     *
     * @return a {@link BaseIOLog} object.
     */
    public BaseIOLog getIoLog() {
        return ioLog;
    }

    /**
     * <p>Setter for the field <code>ioLog</code>.</p>
     *
     * @param ioLog a {@link BaseIOLog} object.
     */
    public void setIoLog(BaseIOLog ioLog) {
        this.ioLog = ioLog;
    }

    /**
     * <p>Getter for the field <code>ePoll</code>.</p>
     *
     * @return a {@link InputStreamEPollWrapper} object.
     */
    public InputStreamEPollWrapper getePoll() {
        return ePoll;
    }

    /**
     * <p>Setter for the field <code>ePoll</code>.</p>
     *
     * @param ePoll a {@link InputStreamEPollWrapper} object.
     */
    public void setePoll(InputStreamEPollWrapper ePoll) {
        this.ePoll = ePoll;
    }

    /**
     * Useful for sending a number of polling commands at once, or at least in as optimal a batch as possible.
     *
     * @param batch a {@link BatchRead} object.
     * @param <K>   type of result
     * @return a {@link BatchResults} object.
     * @throws ModbusTransportException if any.
     * @throws ErrorResponseException   if any.
     */
    public <K> BatchResults<K> send(BatchRead<K> batch) throws ModbusTransportException, ErrorResponseException {
        if (!initialized)
            throw new ModbusTransportException("not initialized");

        BatchResults<K> results = new BatchResults<>();
        List<ReadFunctionGroup<K>> functionGroups = batch.getReadFunctionGroups(this);

        // Execute each read function and process the results.
        for (ReadFunctionGroup<K> functionGroup : functionGroups) {
            sendFunctionGroup(functionGroup, results, batch.isErrorsInResults(), batch.isExceptionsInResults());
            if (batch.isCancel())
                break;
        }

        return results;
    }

    //
    //
    // Protected methods
    //

    /**
     * <p>getMessageControl.</p>
     *
     * @return a {@link MessageControl} object.
     */
    protected MessageControl getMessageControl() {
        MessageControl conn = new MessageControl();
        conn.setRetries(getRetries());
        conn.setTimeout(getTimeout());
        conn.setDiscardDataDelay(getDiscardDataDelay());
        conn.setExceptionHandler(getExceptionHandler());
        conn.setIoLog(ioLog);
        return conn;
    }

    /**
     * <p>closeMessageControl.</p>
     *
     * @param conn a {@link MessageControl} object.
     */
    protected void closeMessageControl(MessageControl conn) {
        if (conn != null)
            conn.close();
    }

    //
    //
    // Private stuff
    //

    /**
     * This method assumes that all locators have already been pre-sorted and grouped into valid requests, say, by the
     * createRequestGroups method.
     */
    private <K> void sendFunctionGroup(ReadFunctionGroup<K> functionGroup, BatchResults<K> results,
                                       boolean errorsInResults, boolean exceptionsInResults) throws ModbusTransportException,
            ErrorResponseException {
        int slaveId = functionGroup.getSlaveAndRange().getSlaveId();
        int startOffset = functionGroup.getStartOffset();
        int length = functionGroup.getLength();

        // Inspect the function group for data required to create the request.
        ModbusRequest request;
        if (functionGroup.getFunctionCode() == FunctionCode.READ_COILS)
            request = new ReadCoilsRequest(slaveId, startOffset, length);
        else if (functionGroup.getFunctionCode() == FunctionCode.READ_DISCRETE_INPUTS)
            request = new ReadDiscreteInputsRequest(slaveId, startOffset, length);
        else if (functionGroup.getFunctionCode() == FunctionCode.READ_HOLDING_REGISTERS)
            request = new ReadHoldingRegistersRequest(slaveId, startOffset, length);
        else if (functionGroup.getFunctionCode() == FunctionCode.READ_INPUT_REGISTERS)
            request = new ReadInputRegistersRequest(slaveId, startOffset, length);
        else
            throw new RuntimeException("Unsupported function");

        ReadResponse response;
        try {
            response = (ReadResponse) send(request);
        } catch (ModbusTransportException e) {
            if (!exceptionsInResults)
                throw e;

            for (KeyedModbusLocator<K> locator : functionGroup.getLocators())
                results.addResult(locator.getKey(), e);

            return;
        }

        byte[] data = null;
        if (!errorsInResults && response.isException())
            throw new ErrorResponseException(request, response);
        else if (!response.isException())
            data = response.getData();

        for (KeyedModbusLocator<K> locator : functionGroup.getLocators()) {
            if (errorsInResults && response.isException())
                results.addResult(locator.getKey(), new ExceptionResult(response.getExceptionCode()));
            else {
                try {
                    results.addResult(locator.getKey(), locator.bytesToValue(data, startOffset));
                } catch (RuntimeException e) {
                    throw new RuntimeException("Result conversion exception. data=" + ArrayUtils.toHexString(data)
                            + ", startOffset=" + startOffset + ", locator=" + locator + ", functionGroup.functionCode="
                            + functionGroup.getFunctionCode() + ", functionGroup.startOffset=" + startOffset
                            + ", functionGroup.length=" + length, e);
                }
            }
        }
    }

    private void setValue(ModbusRequest request) throws ModbusTransportException, ErrorResponseException {
        ModbusResponse response = send(request);
        if (response == null)
            // This should only happen if the request was a broadcast
            return;
        if (response.isException())
            throw new ErrorResponseException(request, response);
    }

    private void setHoldingRegisterBit(int slaveId, int writeOffset, int bit, boolean value)
            throws ModbusTransportException, ErrorResponseException {
        // Writing a bit in a holding register field. There are two ways to do this. The easy way is to
        // use a write mask request, but it is not always supported. The hard way is to read the value, change
        // the appropriate bit, and then write it back again (so as not to overwrite the other bits in the
        // value). However, since the hard way is not atomic, it is not fail-safe either, but it should be
        // at least possible.
        SlaveProfile sp = getSlaveProfile(slaveId);
        if (sp.getWriteMaskRegister()) {
            // Give the write mask a try.
            WriteMaskRegisterRequest request = new WriteMaskRegisterRequest(slaveId, writeOffset);
            request.setBit(bit, value);
            ModbusResponse response = send(request);
            if (response == null)
                // This should only happen if the request was a broadcast
                return;
            if (!response.isException())
                // Hey, cool, it worked.
                return;

            if (response.getExceptionCode() == ExceptionCode.ILLEGAL_FUNCTION)
                // The function is probably not supported. Fail-over to the two step.
                sp.setWriteMaskRegister(false);
            else
                throw new ErrorResponseException(request, response);
        }

        // Do it the hard way. Get the register's current value.
        int regValue = (Integer) getValue(new NumericLocator(slaveId, RegisterRange.HOLDING_REGISTER, writeOffset,
                DataType.TWO_BYTE_INT_UNSIGNED));

        // Modify the value according to the given bit and value.
        if (value)
            regValue = regValue | 1 << bit;
        else
            regValue = regValue & ~(1 << bit);

        // Write the new register value.
        setValue(new WriteRegisterRequest(slaveId, writeOffset, regValue));
    }

    private SlaveProfile getSlaveProfile(int slaveId) {
        SlaveProfile sp = slaveProfiles.get(slaveId);
        if (sp == null) {
            sp = new SlaveProfile();
            slaveProfiles.put(slaveId, sp);
        }
        return sp;
    }


}
