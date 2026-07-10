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
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Omron FINS protocol driver service implementation.
 * <p>
 * Communicates with Omron PLCs via the FINS protocol over TCP sockets.
 * Builds FINS frames manually with proper headers and memory read/write
 * commands. Supports D, W, H, and C memory areas.
 * </p>
 * <p>
 * FINS frame structure (for TCP, a 4-byte length prefix is prepended):
 * </p>
 * <ul>
 *   <li>FINS Header: ICF(0x80) + RSV(0x00) + GCT(0x02) + DNA + DA1 + DA2 + SNA + SA1 + SA2 + SID</li>
 *   <li>FINS Command: MRC + SRC + area/destination code + address + length</li>
 * </ul>
 * <p>
 * Memory area codes: D=0x82, W=0xB1, H=0xB0, C=0x83
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
@Service
public class FinsDriverCustomServiceImpl implements DriverCustomService {

    private static final byte FINS_ICF = (byte) 0x80;
    private static final byte FINS_RSV = (byte) 0x00;
    private static final byte FINS_GCT = (byte) 0x02;

    private static final byte MRC_MEMORY_READ = 0x01;
    private static final byte SRC_MEMORY_READ = 0x01;
    private static final byte MRC_MEMORY_WRITE = 0x01;
    private static final byte SRC_MEMORY_WRITE = 0x02;

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;

    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, Socket> clientMap;

    public FinsDriverCustomServiceImpl(DriverMetadata driverMetadata,
                                       DriverSenderService driverSenderService) {
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    private static void checkRequired(Map<String, AttributeBO> config, String code,
                                      List<ValidationReport.AttributeIssue> issues) {
        AttributeBO attr = config.get(code);
        if (attr == null || attr.getValue() == null) {
            issues.add(ValidationReport.AttributeIssue.builder()
                    .attributeCode(code).level(ValidationReport.IssueLevel.ERROR)
                    .message("Missing required attribute: " + code).build());
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
        Socket socket = clientMap.get(device.getId());
        if (Objects.nonNull(socket) && socket.isConnected() && !socket.isClosed()) {
            return DeviceHealthState.online();
        }
        socket = getConnector(device.getId(), driverConfig);
        return Objects.nonNull(socket) ? DeviceHealthState.online() : DeviceHealthState.offline();
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                Socket removed = clientMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    closeSocket(removed);
                    log.info("Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                            driverCode, metadataEvent.getId(), operateType);
                }
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        Socket socket = getConnector(device.getId(), driverConfig);
        String memoryArea = getConfigValue(pointConfig, "memoryArea", "D");
        int address = getConfigIntValue(pointConfig, "address", 0);
        String dataType = getConfigValue(pointConfig, "dataType", "UINT16");

        try {
            byte areaCode = resolveMemoryAreaCode(memoryArea);
            byte[] finsFrame = buildMemoryReadFrame(driverConfig, areaCode, address, 1);
            byte[] response = sendFinsCommand(socket, finsFrame);

            String value = parseReadResponse(response, dataType);
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            clientMap.remove(device.getId());
            closeSocket(socket);
            throw new ReadPointException("FINS read failed, protocol={}, memoryArea={}, address={}, message={}",
                    driverCode, memoryArea, address, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        Socket socket = getConnector(device.getId(), driverConfig);
        String memoryArea = getConfigValue(pointConfig, "memoryArea", "D");
        int address = getConfigIntValue(pointConfig, "address", 0);
        String dataType = getConfigValue(pointConfig, "dataType", "UINT16");

        try {
            String value = writePointValue.getValue(String.class);
            byte areaCode = resolveMemoryAreaCode(memoryArea);
            byte[] writeData = encodeWriteData(value, dataType);
            byte[] finsFrame = buildMemoryWriteFrame(driverConfig, areaCode, address, writeData);
            sendFinsCommand(socket, finsFrame);
            return true;
        } catch (Exception e) {
            clientMap.remove(device.getId());
            closeSocket(socket);
            throw new WritePointException("FINS write failed, protocol={}, memoryArea={}, address={}, message={}",
                    driverCode, memoryArea, address, e.getMessage(), e);
        }
    }

    /**
     * Get or create a socket connection for the given device.
     *
     * @param deviceId     unique device identifier
     * @param driverConfig driver configuration
     * @return cached or newly created Socket
     */
    private Socket getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return clientMap.computeIfAbsent(deviceId, id -> {
            String host = getConfigValue(driverConfig, "host", "127.0.0.1");
            int port = getConfigIntValue(driverConfig, "port", 9600);
            int timeout = getConfigIntValue(driverConfig, "timeout", 5000);
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), timeout);
                socket.setSoTimeout(timeout);
                log.info("Driver FINS connection established, protocol={}, deviceId={}, host={}:{}",
                        driverCode, deviceId, host, port);
                return socket;
            } catch (IOException e) {
                log.error("Driver FINS connection failed, protocol={}, deviceId={}, host={}:{}, message={}",
                        driverCode, deviceId, host, port, e.getMessage());
                return null;
            }
        });
    }

    /**
     * Build a FINS memory read frame.
     *
     * @param driverConfig driver configuration
     * @param areaCode     memory area code
     * @param address      memory address
     * @param length       number of items to read
     * @return FINS frame bytes
     */
    private byte[] buildMemoryReadFrame(Map<String, AttributeBO> driverConfig,
                                        byte areaCode, int address, int length) {
        int srcNode = getConfigIntValue(driverConfig, "sourceNode", 1);
        int destNode = getConfigIntValue(driverConfig, "destNode", 2);
        int srcUnit = getConfigIntValue(driverConfig, "sourceUnit", 0);
        int destUnit = getConfigIntValue(driverConfig, "destUnit", 0);

        // FINS header (10 bytes) + command (2 bytes) + read params (4 bytes) = 16 bytes total
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.put(FINS_ICF);
        buffer.put(FINS_RSV);
        buffer.put(FINS_GCT);
        buffer.put((byte) destNode);
        buffer.put((byte) destUnit);
        buffer.put((byte) 0x00); // DA2 (destination unit number high byte — typically 0)
        buffer.put((byte) srcNode);
        buffer.put((byte) srcUnit);
        buffer.put((byte) 0x00); // SA2
        buffer.put((byte) 0x01); // SID

        buffer.put(MRC_MEMORY_READ);
        buffer.put(SRC_MEMORY_READ);
        buffer.put(areaCode);
        // Address: 2 bytes big-endian
        buffer.putShort((short) address);
        buffer.put((byte) 0x00); // bit position
        buffer.putShort((short) length);

        return wrapWithTcpHeader(buffer.array());
    }

    /**
     * Build a FINS memory write frame.
     *
     * @param driverConfig driver configuration
     * @param areaCode     memory area code
     * @param address      memory address
     * @param data         data bytes to write
     * @return FINS frame bytes
     */
    private byte[] buildMemoryWriteFrame(Map<String, AttributeBO> driverConfig,
                                         byte areaCode, int address, byte[] data) {
        int srcNode = getConfigIntValue(driverConfig, "sourceNode", 1);
        int destNode = getConfigIntValue(driverConfig, "destNode", 2);
        int srcUnit = getConfigIntValue(driverConfig, "sourceUnit", 0);
        int destUnit = getConfigIntValue(driverConfig, "destUnit", 0);

        // FINS header (10) + command (2) + write params (6) + data = variable
        int frameSize = 10 + 2 + 6 + data.length;
        ByteBuffer buffer = ByteBuffer.allocate(frameSize);
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.put(FINS_ICF);
        buffer.put(FINS_RSV);
        buffer.put(FINS_GCT);
        buffer.put((byte) destNode);
        buffer.put((byte) destUnit);
        buffer.put((byte) 0x00); // DA2
        buffer.put((byte) srcNode);
        buffer.put((byte) srcUnit);
        buffer.put((byte) 0x00); // SA2
        buffer.put((byte) 0x01); // SID

        buffer.put(MRC_MEMORY_WRITE);
        buffer.put(SRC_MEMORY_WRITE);
        buffer.put(areaCode);
        buffer.putShort((short) address);
        buffer.put((byte) 0x00); // bit position
        buffer.putShort((short) data.length);
        buffer.put(data);

        return wrapWithTcpHeader(buffer.array());
    }

    /**
     * Wrap FINS frame with TCP header (4-byte length prefix).
     *
     * @param finsFrame the raw FINS frame
     * @return frame with TCP length prefix
     */
    private byte[] wrapWithTcpHeader(byte[] finsFrame) {
        ByteBuffer wrapped = ByteBuffer.allocate(4 + finsFrame.length);
        wrapped.order(ByteOrder.BIG_ENDIAN);
        wrapped.putInt(finsFrame.length);
        wrapped.put(finsFrame);
        return wrapped.array();
    }

    /**
     * Send a FINS command frame and receive the response.
     *
     * @param socket    the TCP socket
     * @param finsFrame the FINS command frame
     * @return response data bytes
     * @throws IOException on I/O error
     */
    private byte[] sendFinsCommand(Socket socket, byte[] finsFrame) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write(finsFrame);
        out.flush();

        InputStream in = socket.getInputStream();
        // Read TCP length header (4 bytes)
        byte[] lengthHeader = new byte[4];
        int read = in.read(lengthHeader);
        if (read != 4) {
            throw new ReadPointException("FINS response TCP header incomplete, bytesRead={}", read);
        }
        int responseLength = ByteBuffer.wrap(lengthHeader).order(ByteOrder.BIG_ENDIAN).getInt();
        if (responseLength <= 0 || responseLength > 2048) {
            throw new ReadPointException("FINS invalid response length: {}", responseLength);
        }

        // Read response payload
        byte[] response = new byte[responseLength];
        int totalRead = 0;
        while (totalRead < responseLength) {
            read = in.read(response, totalRead, responseLength - totalRead);
            if (read < 0) {
                throw new ReadPointException("FINS response stream ended prematurely");
            }
            totalRead += read;
        }
        return response;
    }

    /**
     * Parse memory read response.
     *
     * @param response FINS response bytes
     * @param dataType expected data type
     * @return parsed value as string
     */
    private String parseReadResponse(byte[] response, String dataType) {
        // FINS response: FINS header (10) + command (2) + end code (2) + data
        // Data starts at byte 14 (index 14) in the raw FINS frame (without TCP header)
        int dataOffset = 14;
        if (response.length <= dataOffset) {
            throw new ReadPointException("FINS response too short, length={}", response.length);
        }

        // Check end code
        int endCode = ((response[12] & 0xFF) << 8) | (response[13] & 0xFF);
        if (endCode != 0) {
            throw new ReadPointException("FINS command failed, endCode=0x{}", Integer.toHexString(endCode));
        }

        // Extract data bytes
        int dataLen = response.length - dataOffset;
        byte[] data = new byte[dataLen];
        System.arraycopy(response, dataOffset, data, 0, dataLen);

        return decodeValue(data, dataType);
    }

    /**
     * Decode a value from raw bytes based on data type.
     *
     * @param data     raw bytes
     * @param dataType data type identifier
     * @return decoded value string
     */
    private String decodeValue(byte[] data, String dataType) {
        return switch (dataType.toUpperCase()) {
            case "INT16" -> String.valueOf(ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getShort());
            case "UINT16" ->
                    String.valueOf(Short.toUnsignedInt(ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getShort()));
            case "INT32" -> String.valueOf(ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getInt());
            case "UINT32" ->
                    String.valueOf(Integer.toUnsignedLong(ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getInt()));
            case "FLOAT" -> String.valueOf(ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getFloat());
            case "STRING" -> new String(data).trim();
            case "BCD" -> bytesToBcdString(data);
            default -> bytesToHex(data);
        };
    }

    /**
     * Encode a string value to bytes for FINS write operations.
     *
     * @param value    string value
     * @param dataType data type identifier
     * @return encoded bytes
     */
    private byte[] encodeWriteData(String value, String dataType) {
        byte[] data;
        switch (dataType.toUpperCase()) {
            case "INT16":
            case "UINT16": {
                data = new byte[2];
                short s = Short.parseShort(value);
                ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).putShort(s);
                break;
            }
            case "INT32":
            case "UINT32":
            case "FLOAT": {
                data = new byte[4];
                int i = Integer.parseInt(value);
                ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).putInt(i);
                break;
            }
            case "STRING": {
                data = value.getBytes();
                break;
            }
            default: {
                data = hexToBytes(value);
                break;
            }
        }
        return data;
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
            default -> (byte) 0x82;
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

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private byte[] hexToBytes(String hex) {
        String s = hex.replaceAll("\\s+", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
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
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
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
        checkRequired(driverConfig, "protocol", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "memoryArea", issues);
        checkRequired(pointConfig, "address", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

}