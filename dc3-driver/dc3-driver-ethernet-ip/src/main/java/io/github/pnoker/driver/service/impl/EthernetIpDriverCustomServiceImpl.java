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
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * EtherNet/IP CIP driver service implementation.
 * <p>
 * Communicates with Rockwell Allen-Bradley PLCs over EtherNet/IP: every device
 * gets a TCP connection with a registered CIP session (command {@code 0x00F0}),
 * and tag reads/writes travel as unconnected messages ({@code sendRRData},
 * command {@code 0x005F}) carrying the CIP message-router Read Tag ({@code 0x4C})
 * and Write Tag ({@code 0x4D}) services. The configured {@code slot} is encoded
 * into the connection path (backplane port segment) so requests reach the
 * controller in that chassis slot.
 * </p>
 * <p>
 * Connection-oriented CIP ({@code ForwardOpen}) is deliberately out of scope for
 * this build: unconnected messaging covers tag reads and writes, and the
 * connection-oriented I/O association is tracked as a separate enhancement.
 * STRING tag writes require structured type support and are rejected with a
 * clear error; STRING reads are decoded from the reply type.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
public class EthernetIpDriverCustomServiceImpl implements DriverCustomService {

    private static final int COMMAND_REGISTER_SESSION = 0x00F0;
    private static final int COMMAND_UNREGISTER_SESSION = 0x00FE;
    private static final int COMMAND_SEND_RR_DATA = 0x005F;
    private static final int ENCAPSULATION_HEADER_SIZE = 24;
    private static final int ITEM_TYPE_NULL_ADDRESS = 0x0000;
    private static final int ITEM_TYPE_UNCONNECTED_DATA = 0x00B1;
    private static final int SERVICE_READ_TAG = 0x4C;
    private static final int SERVICE_WRITE_TAG = 0x4D;
    private static final int CIP_REPLY_FLAG = 0x80;
    private static final int CIP_TYPE_STRING = 0xA0;

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    private final DriverProperties driverProperties;

    private Map<Long, CipSession> connectMap = new ConcurrentHashMap<>(16);

    /** Create the driver custom service. */
    public EthernetIpDriverCustomServiceImpl(
            DriverMetadata driverMetadata, DriverSenderService driverSenderService, DriverProperties driverProperties) {
        this.driverProperties = driverProperties;
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    /** One registered CIP session per device: a socket plus its session handle. */
    private static final class CipSession {
        private final Socket socket;
        private final int sessionHandle;

        private CipSession(Socket socket, int sessionHandle) {
            this.socket = socket;
            this.sessionHandle = sessionHandle;
        }

        private boolean isConnected() {
            return Objects.nonNull(socket) && socket.isConnected() && !socket.isClosed();
        }
    }

    private static void readFully(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = in.read(buffer, offset, buffer.length - offset);
            if (read < 0) throw new IOException("Connection closed");
            offset += read;
        }
    }

    private static byte[] readFully(InputStream in, int length) throws IOException {
        byte[] buffer = new byte[length];
        readFully(in, buffer);
        return buffer;
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

    private static byte[] concat(byte[] left, byte[] right) {
        byte[] joined = new byte[left.length + right.length];
        System.arraycopy(left, 0, joined, 0, left.length);
        System.arraycopy(right, 0, joined, left.length, right.length);
        return joined;
    }

    /**
     * Build the 24-byte EtherNet/IP encapsulation header: command (u16), length
     * (u16), session handle (u32), status (u32), sender context (8 bytes), and
     * options (u32), all little-endian.
     */
    static byte[] buildEncapsulationHeader(int command, int payloadLength, int sessionHandle) {
        ByteBuffer buf = ByteBuffer.allocate(ENCAPSULATION_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) command);
        buf.putShort((short) payloadLength);
        buf.putInt(sessionHandle);
        buf.putInt(0);
        buf.putLong(0);
        buf.putInt(0);
        return buf.array();
    }

    /**
     * Build the Register Session payload: protocol version (1) and zero option
     * flags.
     */
    static byte[] buildRegisterSessionPayload() {
        return ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) 1)
                .putShort((short) 0)
                .array();
    }

    /**
     * Build a full Register Session request: encapsulation header for
     * {@code 0x00F0} wrapping {@link #buildRegisterSessionPayload()}.
     */
    static byte[] buildRegisterSessionRequest() {
        return concat(buildEncapsulationHeader(COMMAND_REGISTER_SESSION, 4, 0), buildRegisterSessionPayload());
    }

    /**
     * Build the {@code sendRRData} encapsulation payload: interface handle,
     * timeout, and two items — a null address item and the unconnected data item
     * wrapping the CIP message-router request.
     */
    static byte[] buildSendRrDataPayload(byte[] cipData) {
        int payloadLength = 8 + 8 + (4 + cipData.length);
        ByteBuffer payload = ByteBuffer.allocate(payloadLength).order(ByteOrder.LITTLE_ENDIAN);
        payload.putInt(0);
        payload.putShort((short) 10);
        payload.putShort((short) 2);
        payload.putShort((short) ITEM_TYPE_NULL_ADDRESS);
        payload.putShort((short) 4);
        payload.putInt(0);
        payload.putShort((short) ITEM_TYPE_UNCONNECTED_DATA);
        payload.putShort((short) cipData.length);
        payload.put(cipData);
        return payload.array();
    }

    /**
     * Build the CIP connection path for a controller tag: a backplane port
     * segment addressing the configured slot, followed by the ANSI extended
     * symbol segment carrying the tag name, padded to a word boundary.
     */
    static byte[] buildConnectionPath(int slot, String tagName) {
        byte[] name = tagName.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buf = ByteBuffer.allocate(4 + name.length + 1);
        buf.put((byte) 0x01).put((byte) slot);
        buf.put((byte) 0x91).put((byte) name.length).put(name);
        if (buf.position() % 2 != 0) {
            buf.put((byte) 0x00);
        }
        return Arrays.copyOf(buf.array(), buf.position());
    }

    /**
     * Build the CIP message-router Read Tag request: service {@code 0x4C}, the
     * connection path, and the element count.
     */
    static byte[] buildMrReadTagRequest(int slot, String tagName, int elementCount) {
        byte[] path = buildConnectionPath(slot, tagName);
        ByteBuffer buf = ByteBuffer.allocate(2 + path.length + 2).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) SERVICE_READ_TAG);
        buf.put((byte) (path.length / 2));
        buf.put(path);
        buf.putShort((short) elementCount);
        return buf.array();
    }

    /**
     * Build the CIP message-router Write Tag request: service {@code 0x4D}, the
     * connection path, the atomic CIP type code, an element count of one, and
     * the encoded value.
     */
    static byte[] buildMrWriteTagRequest(int slot, String tagName, byte typeCode, byte[] value) {
        byte[] path = buildConnectionPath(slot, tagName);
        ByteBuffer buf = ByteBuffer.allocate(2 + path.length + 3 + value.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) SERVICE_WRITE_TAG);
        buf.put((byte) (path.length / 2));
        buf.put(path);
        buf.put(typeCode);
        buf.putShort((short) 1);
        buf.put(value);
        return buf.array();
    }

    /**
     * Extract the unconnected data item ({@code 0x00B1}) — the message-router
     * reply — from a {@code sendRRData} encapsulation payload.
     */
    static byte[] extractUnconnectedDataItem(byte[] rrDataPayload) {
        ByteBuffer buf = ByteBuffer.wrap(rrDataPayload).order(ByteOrder.LITTLE_ENDIAN);
        buf.getInt();
        buf.getShort();
        int itemCount = buf.getShort() & 0xFFFF;
        for (int i = 0; i < itemCount; i++) {
            int type = buf.getShort() & 0xFFFF;
            int length = buf.getShort() & 0xFFFF;
            if (type == ITEM_TYPE_UNCONNECTED_DATA) {
                byte[] data = new byte[length];
                buf.get(data);
                return data;
            }
            buf.position(buf.position() + length);
        }
        throw new ConnectorException("EtherNet/IP reply carries no unconnected data item");
    }

    /**
     * Validate a CIP message-router reply: the reply service must echo the
     * request service with the reply flag set, and the status byte must be zero.
     */
    static void assertMrReplySuccess(byte[] mrReply, int requestService) {
        if (Objects.isNull(mrReply) || mrReply.length < 3) {
            throw new ConnectorException("EtherNet/IP CIP reply is truncated");
        }
        if ((mrReply[0] & 0xFF) != (requestService | CIP_REPLY_FLAG)) {
            throw new ConnectorException(
                    "EtherNet/IP CIP reply carries unexpected service 0x{}", Integer.toHexString(mrReply[0] & 0xFF));
        }
        int status = mrReply[2] & 0xFF;
        if (status != 0) {
            throw new ConnectorException("EtherNet/IP CIP service failed, status=0x{}", Integer.toHexString(status));
        }
    }

    /** The typed payload of a message-router reply: everything after its 3-byte header. */
    static byte[] mrReplyPayload(byte[] mrReply) {
        return Arrays.copyOfRange(mrReply, 3, mrReply.length);
    }

    @Override
    public void initial() {
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // No custom schedule needed.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        CipSession session = connectMap.get(device.getId());
        return Objects.nonNull(session) && session.isConnected()
                ? DeviceHealthState.online()
                : DeviceHealthState.offline();
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
                CipSession removed =
                        Objects.isNull(metadataEvent.getId()) ? null : connectMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    closeSession(removed, metadataEvent.getId());
                    log.info(
                            "Driver connection invalidated, protocol={}, deviceId={}, operateType={}",
                            driverProperties.getCode(),
                            metadataEvent.getId(),
                            operateType);
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
        CipSession session = getConnector(device.getId(), driverConfig);
        try {
            String tagName = getRequiredConfig(pointConfig, "tagName");
            int slot = getConfigIntValue(driverConfig, "slot", 0);

            byte[] mrReply = exchangeUnconnected(session, buildMrReadTagRequest(slot, tagName, 1), SERVICE_READ_TAG);
            String value = parseTagValue(mrReplyPayload(mrReply));

            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            invalidateConnector(device.getId(), session);
            throw e;
        } catch (Exception e) {
            invalidateConnector(device.getId(), session);
            throw new ReadPointException(
                    "EtherNet/IP read failed, protocol={}, message={}", driverProperties.getCode(), e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point,
            WritePointValue writePointValue) {
        CipSession session = getConnector(device.getId(), driverConfig);
        try {
            String tagName = getRequiredConfig(pointConfig, "tagName");
            int slot = getConfigIntValue(driverConfig, "slot", 0);
            String tagType = getConfigValue(pointConfig, "tagType", "DINT");
            String value = writePointValue.getValue(String.class);

            byte[] request =
                    buildMrWriteTagRequest(slot, tagName, mapTypeCode(tagType), encodeTagValue(tagType, value));
            exchangeUnconnected(session, request, SERVICE_WRITE_TAG);
            return true;
        } catch (Exception e) {
            invalidateConnector(device.getId(), session);
            throw new WritePointException(
                    "EtherNet/IP write failed, protocol={}, message={}", driverProperties.getCode(), e.getMessage(), e);
        }
    }

    private CipSession getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return connectMap.computeIfAbsent(deviceId, id -> connect(id, driverConfig));
    }

    private CipSession connect(Long deviceId, Map<String, AttributeBO> driverConfig) {
        String host = getRequiredConfig(driverConfig, "host");
        int port = getConfigIntValue(driverConfig, "port", 44818);
        int timeout = getConfigIntValue(driverConfig, "timeout", 5000);
        int slot = getConfigIntValue(driverConfig, "slot", 0);

        Socket socket;
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(timeout);
        } catch (IOException e) {
            throw new ConnectorException(
                    "EtherNet/IP connection failed, protocol={}, deviceId={}, message={}",
                    driverProperties.getCode(),
                    deviceId,
                    e.getMessage(),
                    e);
        }
        try {
            int sessionHandle = registerSession(socket);
            log.info(
                    "EtherNet/IP session registered, protocol={}, deviceId={}, host={}:{}, slot={}, session=0x{}",
                    driverProperties.getCode(),
                    deviceId,
                    host,
                    port,
                    slot,
                    Integer.toHexString(sessionHandle));
            return new CipSession(socket, sessionHandle);
        } catch (IOException e) {
            closeQuietly(socket, deviceId);
            throw new ConnectorException(
                    "EtherNet/IP session registration failed, protocol={}, deviceId={}, message={}",
                    driverProperties.getCode(),
                    deviceId,
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Perform the Register Session handshake ({@code 0x00F0}) and return the
     * session handle assigned by the PLC.
     */
    private int registerSession(Socket socket) throws IOException {
        byte[] response = sendEncapsulatedCommand(socket, 0, COMMAND_REGISTER_SESSION, buildRegisterSessionPayload());
        return ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
    }

    /**
     * Send a CIP request as an unconnected message ({@code sendRRData}) and
     * return the validated message-router reply.
     */
    private byte[] exchangeUnconnected(CipSession session, byte[] cipRequest, int requestService) throws IOException {
        byte[] response = sendEncapsulatedCommand(
                session.socket, session.sessionHandle, COMMAND_SEND_RR_DATA, buildSendRrDataPayload(cipRequest));
        ByteBuffer header = ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN);
        int payloadLength = header.getShort(2) & 0xFFFF;
        byte[] rrDataPayload =
                Arrays.copyOfRange(response, ENCAPSULATION_HEADER_SIZE, ENCAPSULATION_HEADER_SIZE + payloadLength);
        byte[] mrReply = extractUnconnectedDataItem(rrDataPayload);
        assertMrReplySuccess(mrReply, requestService);
        return mrReply;
    }

    /**
     * Send one encapsulation command and return the full reply (header plus
     * payload). The reply command must echo the request and the encapsulation
     * status must be zero.
     */
    private byte[] sendEncapsulatedCommand(Socket socket, int sessionHandle, int command, byte[] payload)
            throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write(buildEncapsulationHeader(command, payload.length, sessionHandle));
        out.write(payload);
        out.flush();

        InputStream in = socket.getInputStream();
        byte[] header = new byte[ENCAPSULATION_HEADER_SIZE];
        readFully(in, header);
        ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int replyCommand = buf.getShort(0) & 0xFFFF;
        int dataLength = buf.getShort(2) & 0xFFFF;
        int status = buf.getInt(8);
        if (replyCommand != command) {
            throw new IOException("Unexpected encapsulation reply command 0x" + Integer.toHexString(replyCommand));
        }
        if (status != 0) {
            throw new IOException("Encapsulation command failed, status=0x" + Integer.toHexString(status));
        }
        return concat(header, readFully(in, dataLength));
    }

    private void invalidateConnector(Long deviceId, CipSession session) {
        if (Objects.isNull(session) || !connectMap.remove(deviceId, session)) {
            return;
        }
        closeSession(session, deviceId);
    }

    private void closeSession(CipSession session, Long deviceId) {
        try {
            if (session.isConnected()) {
                OutputStream out = session.socket.getOutputStream();
                out.write(buildEncapsulationHeader(COMMAND_UNREGISTER_SESSION, 0, session.sessionHandle));
                out.flush();
            }
        } catch (IOException e) {
            log.warn(
                    "EtherNet/IP session unregister failed, protocol={}, deviceId={}",
                    driverProperties.getCode(),
                    deviceId,
                    e);
        } finally {
            closeQuietly(session.socket, deviceId);
        }
    }

    private void closeQuietly(Socket socket, Long deviceId) {
        try {
            if (Objects.nonNull(socket) && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.warn(
                    "Driver connection closure failed, protocol={}, deviceId={}",
                    driverProperties.getCode(),
                    deviceId,
                    e);
        }
    }

    /**
     * Decode a Read Tag reply payload (type bytes followed by data) into a
     * string. Atomic types are decoded per their CIP type code; a Logix STRING
     * reply carries the two-byte structured type and a length-prefixed body.
     */
    static String parseTagValue(byte[] typedPayload) {
        if (Objects.isNull(typedPayload) || typedPayload.length < 1) {
            return null;
        }
        ByteBuffer buf = ByteBuffer.wrap(typedPayload).order(ByteOrder.LITTLE_ENDIAN);
        int type = buf.get() & 0xFF;
        if (type == CIP_TYPE_STRING) {
            buf.get();
            int length = buf.getInt();
            byte[] chars = new byte[Math.min(length, buf.remaining())];
            buf.get(chars);
            String decoded = new String(chars, StandardCharsets.US_ASCII);
            int terminator = decoded.indexOf(0);
            return terminator >= 0 ? decoded.substring(0, terminator) : decoded;
        }
        return switch (type) {
            case 0xC1 -> String.valueOf(buf.get() != 0);
            case 0xC2 -> String.valueOf(buf.get());
            case 0xC3 -> String.valueOf(buf.getShort());
            case 0xC4 -> String.valueOf(buf.getInt());
            case 0xCA -> String.valueOf(buf.getFloat());
            default -> CodecUtil.bytesToHex(typedPayload);
        };
    }

    private byte[] encodeTagValue(String tagType, String value) {
        ByteBuffer buf;
        switch (tagType.toUpperCase()) {
            case "BOOL" -> {
                buf = ByteBuffer.allocate(1);
                buf.put((byte) (Boolean.parseBoolean(value) ? 1 : 0));
            }
            case "SINT" -> {
                buf = ByteBuffer.allocate(1);
                byte b;
                try {
                    b = Byte.parseByte(value);
                } catch (NumberFormatException e) {
                    b = 0;
                }
                buf.put(b);
            }
            case "INT" -> {
                buf = ByteBuffer.allocate(2);
                short s;
                try {
                    s = Short.parseShort(value);
                } catch (NumberFormatException e) {
                    s = 0;
                }
                buf.order(ByteOrder.LITTLE_ENDIAN).putShort(s);
            }
            case "DINT" -> {
                buf = ByteBuffer.allocate(4);
                int i;
                try {
                    i = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    i = 0;
                }
                buf.order(ByteOrder.LITTLE_ENDIAN).putInt(i);
            }
            case "REAL" -> {
                buf = ByteBuffer.allocate(4);
                float f;
                try {
                    f = Float.parseFloat(value);
                } catch (NumberFormatException e) {
                    f = 0f;
                }
                buf.order(ByteOrder.LITTLE_ENDIAN).putFloat(f);
            }
            default ->
                throw new ConnectorException(
                        "EtherNet/IP tag type '{}' cannot be written atomically; use BOOL, SINT, INT, DINT or REAL",
                        tagType);
        }
        return buf.array();
    }

    private byte mapTypeCode(String tagType) {
        return switch (tagType.toUpperCase()) {
            case "BOOL" -> (byte) 0xC1;
            case "SINT" -> (byte) 0xC2;
            case "INT" -> (byte) 0xC3;
            case "DINT" -> (byte) 0xC4;
            case "REAL" -> (byte) 0xCA;
            default ->
                throw new ConnectorException(
                        "EtherNet/IP tag type '{}' cannot be written atomically; use BOOL, SINT, INT, DINT or REAL",
                        tagType);
        };
    }

    private String getRequiredConfig(Map<String, AttributeBO> config, String code) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr)
                || Objects.isNull(attr.getValue())
                || attr.getValue().isEmpty()) {
            throw new ConnectorException("Required attribute '{}' is missing", code);
        }
        return attr.getValue(String.class);
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
        checkRequired(driverConfig, "slot", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "tagName", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }
}
