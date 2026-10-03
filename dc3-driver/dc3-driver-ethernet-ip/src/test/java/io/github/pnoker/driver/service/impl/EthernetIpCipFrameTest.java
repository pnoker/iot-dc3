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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.exception.ConnectorException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Golden-frame and loopback tests for the EtherNet/IP CIP stack: request frames
 * are asserted byte-for-byte, replies are decoded from spec-shaped fixtures, and
 * a fake PLC on the loopback interface exercises the full register-session and
 * unconnected read flow over real TCP.
 */
@ExtendWith(MockitoExtension.class)
class EthernetIpCipFrameTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    private EthernetIpDriverCustomServiceImpl service;

    @BeforeEach
    void setUp() {
        DriverProperties driverProperties = new DriverProperties();
        driverProperties.setCode("EthernetIpDriver");
        service = new EthernetIpDriverCustomServiceImpl(driverMetadata, driverSenderService, driverProperties);
    }

    @AfterEach
    void tearDown() {
        service.initial();
    }

    /**
     * A spec-shaped encapsulation reply header with the status bytes writable —
     * the builder under test always zeroes status, so fixtures set it explicitly.
     */
    private static byte[] replyHeader(int command, int length, int sessionHandle, int status) {
        byte[] bytes = EthernetIpDriverCustomServiceImpl.buildEncapsulationHeader(command, length, sessionHandle);
        bytes[8] = (byte) status;
        return bytes;
    }

    @Test
    void encapsulationHeaderIsTwentyFourLittleEndianBytes() {
        byte[] bytes = EthernetIpDriverCustomServiceImpl.buildEncapsulationHeader(0x005F, 0x0013, 0x11223344);

        assertThat(bytes)
                .hasSize(24)
                .containsExactly(
                        0x5F, 0x00, 0x13, 0x00, 0x44, 0x33, 0x22, 0x11, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
    }

    @Test
    void registerSessionRequestCarriesProtocolVersionOne() {
        byte[] bytes = EthernetIpDriverCustomServiceImpl.buildRegisterSessionRequest();

        assertThat(bytes)
                .hasSize(28)
                .containsExactly(
                        (byte) 0xF0,
                        0x00,
                        0x04,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x01,
                        0x00,
                        0x00,
                        0x00);
    }

    @Test
    void readTagRequestFramesServicePathAndElementCount() {
        // port segment [01 00] + symbol segment [91 05 "MyTag"] padded to a word boundary
        byte[] bytes = EthernetIpDriverCustomServiceImpl.buildMrReadTagRequest(0, "MyTag", 1);

        assertThat(bytes)
                .containsExactly(
                        0x4C, 0x05, 0x01, 0x00, (byte) 0x91, 0x05, 0x4D, 0x79, 0x54, 0x61, 0x67, 0x00, 0x01, 0x00);
    }

    @Test
    void writeTagRequestCarriesTypeElementCountAndValue() {
        byte[] bytes = EthernetIpDriverCustomServiceImpl.buildMrWriteTagRequest(
                1, "T", (byte) 0xC4, new byte[] {0x78, 0x56, 0x34, 0x12});

        assertThat(bytes)
                .containsExactly(
                        0x4D,
                        0x03,
                        0x01,
                        0x01,
                        (byte) 0x91,
                        0x01,
                        0x54,
                        0x00,
                        (byte) 0xC4,
                        0x01,
                        0x00,
                        0x78,
                        0x56,
                        0x34,
                        0x12);
    }

    @Test
    void sendRrDataPayloadFramesTheUnconnectedDataItem() {
        byte[] bytes = EthernetIpDriverCustomServiceImpl.buildSendRrDataPayload(new byte[] {(byte) 0xAB, (byte) 0xCD});

        assertThat(bytes)
                .containsExactly(
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x0A,
                        0x00,
                        0x02,
                        0x00,
                        0x00,
                        0x00,
                        0x04,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        (byte) 0xB1,
                        0x00,
                        0x02,
                        0x00,
                        (byte) 0xAB,
                        (byte) 0xCD);
    }

    @Test
    void unconnectedDataItemIsExtractedAndMissingItemsAreRejected() {
        byte[] payload = EthernetIpDriverCustomServiceImpl.buildSendRrDataPayload(new byte[] {0x11, 0x22});

        assertThat(EthernetIpDriverCustomServiceImpl.extractUnconnectedDataItem(payload))
                .containsExactly(0x11, 0x22);
        assertThatThrownBy(() -> EthernetIpDriverCustomServiceImpl.extractUnconnectedDataItem(new byte[16]))
                .isInstanceOf(ConnectorException.class)
                .hasMessageContaining("no unconnected data item");
    }

    @Test
    void mrReplyMustEchoTheServiceAndCarryZeroStatus() {
        assertThatThrownBy(() ->
                        EthernetIpDriverCustomServiceImpl.assertMrReplySuccess(new byte[] {0x4C, 0x00, 0x00}, 0x4C))
                .isInstanceOf(ConnectorException.class)
                .hasMessageContaining("unexpected service");
        assertThatThrownBy(() -> EthernetIpDriverCustomServiceImpl.assertMrReplySuccess(
                        new byte[] {(byte) 0xCC, 0x00, 0x05}, 0x4C))
                .isInstanceOf(ConnectorException.class)
                .hasMessageContaining("status=0x5");
        EthernetIpDriverCustomServiceImpl.assertMrReplySuccess(new byte[] {(byte) 0xCC, 0x00, 0x00}, 0x4C);
    }

    @Test
    void readTagReplyDecodesAtomicAndStringTypes() {
        assertThat(EthernetIpDriverCustomServiceImpl.mrReplyPayload(new byte[] {(byte) 0xCC, 0x00, 0x00, 0x11}))
                .containsExactly(0x11);

        byte[] dint = {(byte) 0xC4, 0x78, 0x56, 0x34, 0x12};
        byte[] bool = {(byte) 0xC1, 0x01};
        byte[] string = {(byte) 0xA0, 0x02, 0x03, 0x00, 0x00, 0x00, 0x41, 0x42, 0x43, 0x00};

        assertThat(EthernetIpDriverCustomServiceImpl.parseTagValue(dint)).isEqualTo("305419896");
        assertThat(EthernetIpDriverCustomServiceImpl.parseTagValue(bool)).isEqualTo("true");
        assertThat(EthernetIpDriverCustomServiceImpl.parseTagValue(string)).isEqualTo("ABC");
    }

    @Test
    void fullReadFlowSucceedsAgainstAFakePlc() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            AtomicReference<Throwable> plcError = new AtomicReference<>();
            CountDownLatch finished = new CountDownLatch(1);
            Thread plc = new Thread(() -> {
                try (Socket connection = server.accept()) {
                    InputStream in = connection.getInputStream();

                    byte[] registerRequest = readN(in, 28);
                    assertThat(registerRequest)
                            .isEqualTo(EthernetIpDriverCustomServiceImpl.buildRegisterSessionRequest());
                    connection.getOutputStream().write(replyHeader(0x00F0, 4, 0x11223344, 0));
                    connection.getOutputStream().write(new byte[] {0x01, 0x00, 0x00, 0x00});

                    byte[] rrHeader = readN(in, 24);
                    int payloadLength = ByteBuffer.wrap(rrHeader)
                                    .order(ByteOrder.LITTLE_ENDIAN)
                                    .getShort(2)
                            & 0xFFFF;
                    byte[] rrPayload = readN(in, payloadLength);
                    assertThat(rrHeader[0] & 0xFF).isEqualTo(0x5F);
                    assertThat(rrPayload)
                            .isEqualTo(EthernetIpDriverCustomServiceImpl.buildSendRrDataPayload(
                                    EthernetIpDriverCustomServiceImpl.buildMrReadTagRequest(0, "MyTag", 1)));

                    byte[] mrReply = {(byte) 0xCC, 0x00, 0x00, (byte) 0xC4, 0x78, 0x56, 0x34, 0x12};
                    byte[] replyPayload = EthernetIpDriverCustomServiceImpl.buildSendRrDataPayload(mrReply);
                    connection.getOutputStream().write(replyHeader(0x005F, replyPayload.length, 0x11223344, 0));
                    connection.getOutputStream().write(replyPayload);
                } catch (Throwable t) {
                    plcError.set(t);
                } finally {
                    finished.countDown();
                }
            });
            plc.start();

            Map<String, AttributeBO> driverConfig = new HashMap<>();
            driverConfig.put("host", attribute("127.0.0.1", AttributeTypeEnum.STRING));
            driverConfig.put("port", attribute(String.valueOf(server.getLocalPort()), AttributeTypeEnum.INT));
            driverConfig.put("slot", attribute("0", AttributeTypeEnum.INT));
            driverConfig.put("timeout", attribute("5000", AttributeTypeEnum.INT));
            Map<String, AttributeBO> pointConfig = new HashMap<>();
            pointConfig.put("tagName", attribute("MyTag", AttributeTypeEnum.STRING));

            DeviceBO device = new DeviceBO();
            device.setId(1L);
            PointBO point = new PointBO();
            point.setId(2L);
            point.setPointTypeFlag(PointTypeEnum.STRING);

            ReadPointValue value = service.read(driverConfig, pointConfig, device, point);

            assertThat(value.getValue()).isEqualTo("305419896");
            assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
            plc.join(1000);
            assertThat(plcError.get()).isNull();
        }
    }

    private static byte[] readN(InputStream in, int length) throws IOException {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(buffer, offset, length - offset);
            if (read < 0) throw new IOException("Fake PLC closed early");
            offset += read;
        }
        return buffer;
    }

    private static AttributeBO attribute(String value, AttributeTypeEnum type) {
        return AttributeBO.builder().value(value).type(type).build();
    }
}
