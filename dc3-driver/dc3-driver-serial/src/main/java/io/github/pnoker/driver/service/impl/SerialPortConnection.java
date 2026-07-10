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

import com.fazecast.jSerialComm.SerialPort;
import io.github.pnoker.common.exception.ConnectorException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Wrapper for a jSerialComm serial port connection.
 * <p>
 * Handles port opening, configuration, byte I/O, and clean shutdown.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
public class SerialPortConnection {

    private final String portName;
    private final int baudRate;
    private final int dataBits;
    private final int stopBits;
    private final int parity;
    private final int timeout;

    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;

    public SerialPortConnection(String portName, int baudRate, int dataBits, int stopBits, int parity, int timeout) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.timeout = timeout;
    }

    /**
     * Open and configure the serial port.
     *
     * @throws ConnectorException if the port cannot be opened
     */
    public void open() {
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.setParity(parity);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);
        serialPort.clearDTR();
        serialPort.clearRTS();
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

        boolean opened = serialPort.openPort();
        if (!opened) {
            throw new ConnectorException("Failed to open serial port: {}", portName);
        }

        inputStream = serialPort.getInputStream();
        outputStream = serialPort.getOutputStream();
        log.info("Serial port opened: {}, baudRate={}, dataBits={}, stopBits={}, parity={}",
                portName, baudRate, dataBits, stopBits, parity);
    }

    /**
     * Send bytes and read the response.
     *
     * @param command     bytes to send
     * @param responseLen expected response length (0 = read until timeout or inter-frame gap)
     * @return response bytes
     * @throws IOException if I/O fails
     */
    public byte[] sendAndReceive(byte[] command, int responseLen) throws IOException {
        if (Objects.isNull(outputStream) || Objects.isNull(inputStream)) {
            throw new IOException("Serial port is not open: " + portName);
        }

        // Flush any stale data
        while (inputStream.available() > 0) {
            inputStream.read();
        }

        // Send command
        outputStream.write(command);
        outputStream.flush();
        log.trace("Serial sent {} bytes to {}", command.length, portName);

        // Read response
        if (responseLen > 0) {
            return readFixedLength(responseLen);
        } else {
            return readUntilTimeout();
        }
    }

    /**
     * Send bytes without waiting for a response.
     *
     * @param command bytes to send
     * @throws IOException if I/O fails
     */
    public void send(byte[] command) throws IOException {
        if (Objects.isNull(outputStream)) {
            throw new IOException("Serial port is not open: " + portName);
        }
        outputStream.write(command);
        outputStream.flush();
        log.trace("Serial sent {} bytes to {}", command.length, portName);
    }

    /**
     * Close the serial port and release resources.
     */
    public void close() {
        try {
            if (Objects.nonNull(inputStream)) {
                inputStream.close();
            }
        } catch (IOException e) {
            log.warn("Failed to close serial input stream: {}", portName, e);
        }
        try {
            if (Objects.nonNull(outputStream)) {
                outputStream.close();
            }
        } catch (IOException e) {
            log.warn("Failed to close serial output stream: {}", portName, e);
        }
        if (Objects.nonNull(serialPort) && serialPort.isOpen()) {
            serialPort.closePort();
            log.info("Serial port closed: {}", portName);
        }
    }

    /**
     * Check if the serial port is open.
     *
     * @return true if open
     */
    public boolean isOpen() {
        return Objects.nonNull(serialPort) && serialPort.isOpen();
    }

    public String getPortName() {
        return portName;
    }

    private byte[] readFixedLength(int length) throws IOException {
        byte[] buffer = new byte[length];
        int totalRead = 0;
        long deadline = System.currentTimeMillis() + timeout;
        while (totalRead < length && System.currentTimeMillis() < deadline) {
            int available = inputStream.available();
            if (available > 0) {
                int toRead = Math.min(available, length - totalRead);
                int read = inputStream.read(buffer, totalRead, toRead);
                if (read > 0) {
                    totalRead += read;
                }
            } else {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while reading serial port", e);
                }
            }
        }
        if (totalRead < length) {
            log.warn("Serial partial read: expected={}, actual={}, port={}", length, totalRead, portName);
        }
        byte[] result = new byte[totalRead];
        System.arraycopy(buffer, 0, result, 0, totalRead);
        return result;
    }

    private byte[] readUntilTimeout() throws IOException {
        byte[] buffer = new byte[4096];
        int totalRead = 0;
        int idleCount = 0;
        int maxIdle = 3;

        while (totalRead < buffer.length && idleCount < maxIdle) {
            int available = inputStream.available();
            if (available > 0) {
                int toRead = Math.min(available, buffer.length - totalRead);
                int read = inputStream.read(buffer, totalRead, toRead);
                if (read > 0) {
                    totalRead += read;
                    idleCount = 0;
                }
            } else {
                idleCount++;
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while reading serial port", e);
                }
            }
        }
        byte[] result = new byte[totalRead];
        System.arraycopy(buffer, 0, result, 0, totalRead);
        return result;
    }

}
