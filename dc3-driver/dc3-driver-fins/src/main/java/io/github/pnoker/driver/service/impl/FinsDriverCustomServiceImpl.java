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

import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.driver.support.CodecUtil;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Omron FINS protocol driver service implementation.
 * <p>
 * Communicates with Omron PLCs over FINS/TCP (Omron W420 "Ethernet" transport).
 * Every TCP exchange carries the standard FINS/TCP envelope: the ASCII magic
 * {@code 'FINS'}, a 4-byte big-endian length counting from the command field,
 * a 4-byte command, and a 4-byte error field. Before the first data exchange
 * the client performs the mandatory node-address registration handshake
 * (command 0x00000000); the PLC answers with command 0x00000001 and assigns
 * the client node address used as the FINS source node (SA1).
 * </p>
 * <p>
 * FINS frame structure (inside the TCP envelope):
 * ICF(0x80) + RSV(0x00) + GCT(0x02) + DNA + DA1 + DA2 + SNA + SA1 + SA2 + SID,
 * followed by the command (MRC + SRC) and its parameters. Memory area codes:
 * D=0x82, W=0xB1, H(HE)=0xB0, C(CIO)=0x83. FINS multi-byte fields are
 * big-endian on the wire.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
public class FinsDriverCustomServiceImpl implements DriverCustomService {

    private static final byte[] FINS_TCP_MAGIC = {'F', 'I', 'N', 'S'};
    private static final int FINS_TCP_LENGTH_OFFSET = 4;
    private static final int FINS_TCP_COMMAND_OFFSET = 8;
    private static final int FINS_TCP_ERROR_OFFSET = 12;
    private static final int FINS_TCP_HEADER_SIZE = 16;
    private static final int FINS_TCP_COMMAND_NODE_REGISTRATION = 0x00000000;
    private static final int FINS_TCP_COMMAND_NODE_ACKNOWLEDGE = 0x00000001;
    private static final int FINS_TCP_COMMAND_FINS_FRAME = 0x00000002;
    /** Hard cap on accepted TCP payload sizes; real FINS frames stay below 2012 bytes. */
    private static final int MAX_TCP_PAYLOAD = 4096;

    private static final byte FINS_ICF = (byte) 0x80;
    private static final byte FINS_RSV = (byte) 0x00;
    private static final byte FINS_GCT = (byte) 0x02;

    private static final byte MRC_MEMORY = 0x01;
    private static final byte SRC_MEMORY_READ = 0x01;
    private static final byte SRC_MEMORY_WRITE = 0x02;

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    private final DriverProperties driverProperties;

    private Map<Long, FinsConnection> clientMap = new ConcurrentHashMap<>(16);
    private final AtomicInteger sidSequence = new AtomicInteger(0);

    /** Create the FINS driver custom service. */
    public FinsDriverCustomServiceImpl(
            DriverMetadata driverMetadata, DriverSenderService driverSenderService, DriverProperties driverProperties) {
        this.driverProperties = driverProperties;
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    private static void checkRequired(
            Map<String, AttributeBO> config, String code, List<ValidationReport.AttributeIssue> issues) {
        AttributeBO attr = config.get(code);
        if (attr == null || attr.getValue() == null) {
            issues.add(ValidationReport.AttributeIssue.builder()
                    .attributeCode(code)
                    .level(ValidationReport.IssueLevel.ERROR)
                    .message("Missing required attribute: " + code)
                    .build());
        }
    }

    @Override
    public void initial() {
        clientMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // FINS drivers do not need custom scheduled tasks.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        FinsConnection connection = clientMap.get(device.getId());
        if (Objects.nonNull(connection)
                && connection.socket().isConnected()
                && !connection.socket().isClosed()) {
            return DeviceHealthState.online();
        }
        try {
            getConnector(device.getId(), driverConfig);
        } catch (Exception e) {
            log.warn(
                    "Driver health check failed, protocol={}, deviceId={}",
                    driverProperties.getCode(),
                    device.getId(),
                    e);
            return DeviceHealthState.offline();
        }
        return DeviceHealthState.online();
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info(
                    "Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverProperties.getCode(),
                    metadataType,
                    operateType,
                    metadataEvent.getId());

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                if (Objects.nonNull(metadataEvent.getId())) {
                    FinsConnection removed = clientMap.remove(metadataEvent.getId());
                    if (Objects.nonNull(removed)) {
                        closeSocket(removed.socket());
                        log.info(
                                "Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                                driverProperties.getCode(),
                                metadataEvent.getId(),
                                operateType);
                    }
                }
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info(
                    "Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverProperties.getCode(),
                    metadataType,
                    operateType,
                    metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point) {
        FinsConnection connection = getConnector(device.getId(), driverConfig);
        String memoryArea = getConfigValue(pointConfig, "memoryArea", "D");
        int address = getConfigIntValue(pointConfig, "address", 0);
        String dataType = getConfigValue(pointConfig, "dataType", "UINT16");

        try {
            byte areaCode = resolveMemoryAreaCode(memoryArea);
            byte[] finsFrame =
                    buildMemoryReadFrame(driverConfig, connection.clientNode(), areaCode, address, wordCount(dataType));
            byte[] response = sendFinsCommand(connection, finsFrame);

            String value = parseReadResponse(response, dataType);
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            clientMap.remove(device.getId());
            closeSocket(connection.socket());
            throw new ReadPointException(
                    "FINS read failed, protocol={}, memoryArea={}, address={}, message={}",
                    driverProperties.getCode(),
                    memoryArea,
                    address,
                    e.getMessage(),
                    e);
        }
    }

    @Override
    public Boolean write(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point,
            WritePointValue writePointValue) {
        FinsConnection connection = getConnector(device.getId(), driverConfig);
        String memoryArea = getConfigValue(pointConfig, "memoryArea", "D");
        int address = getConfigIntValue(pointConfig, "address", 0);
        String dataType = getConfigValue(pointConfig, "dataType", "UINT16");

        try {
            String value = writePointValue.getValue(String.class);
            byte areaCode = resolveMemoryAreaCode(memoryArea);
            byte[] writeData = encodeWriteData(value, dataType);
            byte[] finsFrame =
                    buildMemoryWriteFrame(driverConfig, connection.clientNode(), areaCode, address, writeData);
            byte[] response = sendFinsCommand(connection, finsFrame);
            checkEndCode(response);
            return true;
        } catch (WritePointException e) {
            throw e;
        } catch (Exception e) {
            clientMap.remove(device.getId());
            closeSocket(connection.socket());
            throw new WritePointException(
                    "FINS write failed, protocol={}, memoryArea={}, address={}, message={}",
                    driverProperties.getCode(),
                    memoryArea,
                    address,
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Get or create a FINS/TCP connection for the given device, including the
     * mandatory node-address registration handshake.
     *
     * @param deviceId     unique device identifier
     * @param driverConfig driver configuration
     * @return cached or newly created connection
     */
    private FinsConnection getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return clientMap.computeIfAbsent(deviceId, id -> {
            String host = getConfigValue(driverConfig, "host", "127.0.0.1");
            int port = getConfigIntValue(driverConfig, "port", 9600);
            int timeout = getConfigIntValue(driverConfig, "timeout", 5000);
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), timeout);
                socket.setSoTimeout(timeout);
                int clientNode = performNodeRegistration(socket);
                log.info(
                        "Driver FINS connection established, protocol={}, deviceId={}, host={}:{}, clientNode={}",
                        driverProperties.getCode(),
                        deviceId,
                        host,
                        port,
                        clientNode);
                return new FinsConnection(socket, clientNode);
            } catch (IOException e) {
                log.error(
                        "Driver FINS connection failed, protocol={}, deviceId={}, host={}, port={}",
                        driverProperties.getCode(),
                        deviceId,
                        host,
                        port,
                        e);
                throw new ConnectorException("FINS connect failed, host={}, port={}: {}", host, port, e.getMessage());
            }
        });
    }

    /**
     * Run the FINS/TCP node-address registration handshake and return the
     * client node address assigned by the PLC.
     *
     * @param socket an open, connected socket
     * @return the node address the PLC assigned to this client
     * @throws IOException on I/O or protocol error
     */
    private int performNodeRegistration(Socket socket) throws IOException {
        byte[] request = buildNodeRegistrationFrame();
        writeFrame(socket, request);

        byte[] response = readTcpEnvelope(socket);
        int command = readInt(response, FINS_TCP_COMMAND_OFFSET);
        int error = readInt(response, FINS_TCP_ERROR_OFFSET);
        if (command != FINS_TCP_COMMAND_NODE_ACKNOWLEDGE) {
            throw new IOException(
                    "FINS/TCP node registration got unexpected command 0x" + Integer.toHexString(command));
        }
        if (error != 0) {
            throw new IOException("FINS/TCP node registration failed, error=0x" + Integer.toHexString(error));
        }
        if (response.length < FINS_TCP_HEADER_SIZE + 4) {
            throw new IOException("FINS/TCP node registration response missing client node address");
        }
        return readInt(response, FINS_TCP_HEADER_SIZE);
    }

    /**
     * Build the FINS/TCP node-address registration frame (command 0x00000000).
     *
     * @return the complete registration frame
     */
    byte[] buildNodeRegistrationFrame() {
        ByteBuffer buffer = ByteBuffer.allocate(FINS_TCP_HEADER_SIZE);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(FINS_TCP_MAGIC);
        buffer.putInt(FINS_TCP_HEADER_SIZE - FINS_TCP_COMMAND_OFFSET);
        buffer.putInt(FINS_TCP_COMMAND_NODE_REGISTRATION);
        buffer.putInt(0);
        return buffer.array();
    }

    /**
     * Wrap a raw FINS frame in the FINS/TCP data-send envelope (command 0x00000002).
     *
     * @param finsFrame the raw FINS frame
     * @return frame with the TCP envelope
     */
    byte[] wrapWithTcpHeader(byte[] finsFrame) {
        ByteBuffer wrapped = ByteBuffer.allocate(FINS_TCP_HEADER_SIZE + finsFrame.length);
        wrapped.order(ByteOrder.BIG_ENDIAN);
        wrapped.put(FINS_TCP_MAGIC);
        wrapped.putInt(finsFrame.length + (FINS_TCP_HEADER_SIZE - FINS_TCP_COMMAND_OFFSET));
        wrapped.putInt(FINS_TCP_COMMAND_FINS_FRAME);
        wrapped.putInt(0);
        wrapped.put(finsFrame);
        return wrapped.array();
    }

    /**
     * Build a FINS memory area read command (0101).
     *
     * @param driverConfig driver configuration
     * @param clientNode   node address assigned by the PLC handshake
     * @param areaCode     memory area code
     * @param address      memory address
     * @param length       number of items to read
     * @return raw FINS frame (without the TCP envelope)
     */
    private byte[] buildMemoryReadFrame(
            Map<String, AttributeBO> driverConfig, int clientNode, byte areaCode, int address, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(10 + 2 + 6);
        buffer.order(ByteOrder.BIG_ENDIAN);
        putFinsHeader(buffer, driverConfig, clientNode);
        buffer.put(MRC_MEMORY);
        buffer.put(SRC_MEMORY_READ);
        buffer.put(areaCode);
        buffer.putShort((short) address);
        buffer.put((byte) 0x00); // bit position (word access)
        buffer.putShort((short) length);
        return buffer.array();
    }

    /**
     * Build a FINS memory area write command (0102).
     *
     * @param driverConfig driver configuration
     * @param clientNode   node address assigned by the PLC handshake
     * @param areaCode     memory area code
     * @param address      memory address
     * @param data         data bytes to write
     * @return raw FINS frame (without the TCP envelope)
     */
    private byte[] buildMemoryWriteFrame(
            Map<String, AttributeBO> driverConfig, int clientNode, byte areaCode, int address, byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(10 + 2 + 6 + data.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        putFinsHeader(buffer, driverConfig, clientNode);
        buffer.put(MRC_MEMORY);
        buffer.put(SRC_MEMORY_WRITE);
        buffer.put(areaCode);
        buffer.putShort((short) address);
        buffer.put((byte) 0x00); // bit position (word access)
        buffer.putShort((short) (data.length / 2));
        buffer.put(data);
        return buffer.array();
    }

    /**
     * Write the 10-byte FINS header with corrected address semantics:
     * DNA/DA1/DA2 (destination network/node/unit) then SNA/SA1/SA2 (source).
     *
     * @param buffer      target buffer
     * @param config      driver configuration for node overrides
     * @param clientNode  node address assigned by the PLC handshake (SA1 default)
     */
    private void putFinsHeader(ByteBuffer buffer, Map<String, AttributeBO> config, int clientNode) {
        int destNode = getConfigIntValue(config, "destNode", 2);
        int destUnit = getConfigIntValue(config, "destUnit", 0);
        int srcNode = getConfigIntValue(config, "sourceNode", clientNode);
        int srcUnit = getConfigIntValue(config, "sourceUnit", 0);
        int sid = sidSequence.updateAndGet(current -> current >= 0xFE ? 1 : current + 1);

        buffer.put(FINS_ICF);
        buffer.put(FINS_RSV);
        buffer.put(FINS_GCT);
        buffer.put((byte) 0x00); // DNA — local network
        buffer.put((byte) destNode); // DA1 — destination node address
        buffer.put((byte) destUnit); // DA2 — destination unit (CPU)
        buffer.put((byte) 0x00); // SNA — source network, local
        buffer.put((byte) srcNode); // SA1 — source node address
        buffer.put((byte) srcUnit); // SA2 — source unit (CPU)
        buffer.put((byte) sid);
    }

    /**
     * Send a raw FINS frame in a FINS/TCP envelope and receive the response
     * envelope, returning the raw FINS response frame.
     *
     * @param connection the device connection holding the socket
     * @param finsFrame  the raw FINS command frame
     * @return raw FINS response frame (without the TCP envelope)
     * @throws IOException on I/O or protocol error
     */
    private byte[] sendFinsCommand(FinsConnection connection, byte[] finsFrame) throws IOException {
        // Serialize request/response exchanges: a scheduled read and a command write
        // racing on the same socket would interleave FINS/TCP envelopes.
        connection.lock.lock();
        try {
            Socket socket = connection.socket();
            writeFrame(socket, wrapWithTcpHeader(finsFrame));

            byte[] envelope = readTcpEnvelope(socket);
            int command = readInt(envelope, FINS_TCP_COMMAND_OFFSET);
            int error = readInt(envelope, FINS_TCP_ERROR_OFFSET);
            if (command != FINS_TCP_COMMAND_FINS_FRAME) {
                throw new IOException(
                        "FINS/TCP data response has unexpected command 0x" + Integer.toHexString(command));
            }
            if (error != 0) {
                throw new IOException("FINS/TCP transport error 0x" + Integer.toHexString(error));
            }
            byte[] finsResponse = new byte[envelope.length - FINS_TCP_HEADER_SIZE];
            System.arraycopy(envelope, FINS_TCP_HEADER_SIZE, finsResponse, 0, finsResponse.length);
            return finsResponse;
        } finally {
            connection.lock.unlock();
        }
    }

    private void writeFrame(Socket socket, byte[] frame) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write(frame);
        out.flush();
    }

    /**
     * Read one complete FINS/TCP envelope: 4-byte magic, 4-byte length, then
     * the length-counted payload. Both header reads loop until full because a
     * TCP segment may carry only part of a header.
     *
     * @param socket the TCP socket
     * @return the full envelope (magic + length + payload)
     * @throws IOException on I/O, magic mismatch, or oversized payload
     */
    private byte[] readTcpEnvelope(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        byte[] header = new byte[FINS_TCP_LENGTH_OFFSET + 4];
        readFully(in, header);
        if (header[0] != FINS_TCP_MAGIC[0]
                || header[1] != FINS_TCP_MAGIC[1]
                || header[2] != FINS_TCP_MAGIC[2]
                || header[3] != FINS_TCP_MAGIC[3]) {
            throw new IOException("FINS/TCP response is missing the 'FINS' magic");
        }
        int payloadLength = readInt(header, FINS_TCP_LENGTH_OFFSET);
        if (payloadLength <= 0 || payloadLength > MAX_TCP_PAYLOAD) {
            throw new IOException("FINS invalid response length: " + payloadLength);
        }
        byte[] envelope = new byte[header.length + payloadLength];
        System.arraycopy(header, 0, envelope, 0, header.length);
        readFully(in, envelope, header.length, payloadLength);
        return envelope;
    }

    private void readFully(InputStream in, byte[] buffer) throws IOException {
        readFully(in, buffer, 0, buffer.length);
    }

    private void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        int totalRead = 0;
        while (totalRead < length) {
            int read = in.read(buffer, offset + totalRead, length - totalRead);
            if (read < 0) {
                throw new IOException("FINS response stream ended prematurely");
            }
            totalRead += read;
        }
    }

    private int readInt(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    /**
     * Parse memory read response.
     *
     * @param response raw FINS response frame
     * @param dataType expected data type
     * @return parsed value as string
     */
    private String parseReadResponse(byte[] response, String dataType) {
        // FINS response: FINS header (10) + command (2) + end code (2) + data
        int dataOffset = 14;
        if (response.length <= dataOffset) {
            throw new ReadPointException("FINS response too short, length={}", response.length);
        }
        checkEndCode(response);

        int dataLen = response.length - dataOffset;
        byte[] data = new byte[dataLen];
        System.arraycopy(response, dataOffset, data, 0, dataLen);

        return decodeValue(data, dataType);
    }

    /**
     * Verify the end code of a FINS response frame; a non-zero end code means
     * the PLC rejected the command, and the caller must not treat it as success.
     *
     * @param response raw FINS response frame (header + command + end code)
     */
    private void checkEndCode(byte[] response) {
        if (response.length < 14) {
            throw new ReadPointException("FINS response too short to carry an end code, length={}", response.length);
        }
        int endCode = ((response[12] & 0xFF) << 8) | (response[13] & 0xFF);
        if (endCode != 0) {
            throw new ReadPointException("FINS command failed, endCode=0x{}", Integer.toHexString(endCode));
        }
    }

    /**
     * Decode a value from raw bytes based on data type.
     *
     * @param data     raw bytes
     * @param dataType data type identifier
     * @return decoded value string
     */
    String decodeValue(byte[] data, String dataType) {
        return switch (dataType.toUpperCase()) {
            case "INT16" ->
                String.valueOf(ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getShort());
            case "UINT16" ->
                String.valueOf(Short.toUnsignedInt(
                        ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getShort()));
            case "INT32" ->
                String.valueOf(ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getInt());
            case "UINT32" ->
                String.valueOf(Integer.toUnsignedLong(
                        ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getInt()));
            case "FLOAT" ->
                String.valueOf(ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getFloat());
            case "STRING" -> new String(data, StandardCharsets.US_ASCII).trim();
            case "BCD" -> bytesToBcdString(data);
            default -> CodecUtil.bytesToHex(data);
        };
    }

    /**
     * Resolve the FINS memory read length in words (2 bytes each) for a data type.
     * 32-bit types (INT32/UINT32/FLOAT) span 2 words; all others read a single word.
     *
     * @param dataType data type identifier
     * @return word count to request
     */
    int wordCount(String dataType) {
        return switch (dataType.toUpperCase()) {
            case "INT32", "UINT32", "FLOAT" -> 2;
            default -> 1;
        };
    }

    /**
     * Encode a string value to bytes for FINS write operations. A value that
     * cannot be parsed as the configured type is rejected: silently writing 0
     * would put wrong data into a physical PLC.
     *
     * @param value    string value
     * @param dataType data type identifier
     * @return encoded bytes
     */
    byte[] encodeWriteData(String value, String dataType) {
        switch (dataType.toUpperCase()) {
            case "INT16", "UINT16": {
                short parsed;
                try {
                    parsed = Short.parseShort(value.trim());
                } catch (NumberFormatException e) {
                    throw new WritePointException("FINS write value '{}' is not a valid UINT16", value);
                }
                byte[] data = new byte[2];
                ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).putShort(parsed);
                return data;
            }
            case "INT32", "UINT32": {
                int parsed;
                try {
                    parsed = Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    throw new WritePointException("FINS write value '{}' is not a valid INT32", value);
                }
                byte[] data = new byte[4];
                ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).putInt(parsed);
                return data;
            }
            case "FLOAT": {
                float parsed;
                try {
                    parsed = Float.parseFloat(value.trim());
                } catch (NumberFormatException e) {
                    throw new WritePointException("FINS write value '{}' is not a valid FLOAT", value);
                }
                byte[] data = new byte[4];
                ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).putFloat(parsed);
                return data;
            }
            case "STRING": {
                return value.getBytes(StandardCharsets.US_ASCII);
            }
            default: {
                byte[] data = CodecUtil.hexToBytes(value);
                if (data.length == 0 && value.length() > 0) {
                    throw new WritePointException("FINS write value '{}' is not valid hex", value);
                }
                return data;
            }
        }
    }

    /**
     * Resolve a memory area name to its FINS area code.
     *
     * @param memoryArea area name (D, W, H, C)
     * @return FINS area code byte
     */
    private byte resolveMemoryAreaCode(String memoryArea) {
        return switch (memoryArea.toUpperCase()) {
            case "D" -> (byte) 0x82;
            case "W" -> (byte) 0xB1;
            case "H" -> (byte) 0xB0;
            case "C" -> (byte) 0x83;
            default -> throw new ReadPointException("FINS unsupported memory area '{}'", memoryArea);
        };
    }

    private void closeSocket(Socket socket) {
        try {
            if (Objects.nonNull(socket) && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.warn("FINS socket close failed", e);
        }
    }

    private String bytesToBcdString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append((b >> 4) & 0x0F);
            sb.append(b & 0x0F);
        }
        return sb.toString();
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr)
                || Objects.isNull(attr.getValue())
                || attr.getValue().isEmpty()) {
            return defaultValue;
        }
        return attr.getValue(String.class);
    }

    private int getConfigIntValue(Map<String, AttributeBO> config, String code, int defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue())) {
            return defaultValue;
        }
        return attr.getValue(Integer.class);
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "host", issues);
        checkRequired(driverConfig, "port", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "memoryArea", issues);
        checkRequired(pointConfig, "address", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

    /**
     * Holder pairing a device socket with the node address the PLC assigned
     * during the FINS/TCP registration handshake.
     */
    private static final class FinsConnection {

        private final Socket socket;
        private final int clientNode;
        private final java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();

        private FinsConnection(Socket socket, int clientNode) {
            this.socket = socket;
            this.clientNode = clientNode;
        }

        Socket socket() {
            return socket;
        }

        int clientNode() {
            return clientNode;
        }
    }
}
