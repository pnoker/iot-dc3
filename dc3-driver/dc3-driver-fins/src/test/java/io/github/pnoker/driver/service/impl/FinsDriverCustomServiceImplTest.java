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
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.exception.WritePointException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinsDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    private FinsDriverCustomServiceImpl service;

    @BeforeEach
    void setUp() {
        DriverProperties driverProperties = new DriverProperties();
        driverProperties.setCode("FinsDriver");
        service = new FinsDriverCustomServiceImpl(driverMetadata, driverSenderService, driverProperties);
    }

    @Test
    void validateFlagsMissingRequiredDriverAttributes() {
        ValidationReport report = service.validate(new HashMap<>());

        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).extracting("attributeCode").contains("host", "port");
        report.getIssues().forEach(issue -> assertThat(issue.getLevel()).isEqualTo(ValidationReport.IssueLevel.ERROR));
    }

    @Test
    void validatePointFlagsMissingRequiredPointAttributes() {
        PointBO point = new PointBO();
        point.setId(1L);

        ValidationReport report = service.validatePoint(new HashMap<>(), point);

        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).extracting("attributeCode").contains("memoryArea", "address");
    }

    @Test
    void validatePassesWhenRequiredDriverAttributesPresent() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put(
                "host",
                AttributeBO.builder()
                        .value("127.0.0.1")
                        .type(AttributeTypeEnum.STRING)
                        .build());
        config.put("port", AttributeBO.builder().value("9600").build());

        ValidationReport report = service.validate(config);

        assertThat(report.isPassed()).isTrue();
        assertThat(report.getIssues()).isEmpty();
    }

    @Test
    void wordCountIsTwoFor32BitTypesAndOneOtherwise() {
        assertThat(service.wordCount("INT32")).isEqualTo(2);
        assertThat(service.wordCount("UINT32")).isEqualTo(2);
        assertThat(service.wordCount("FLOAT")).isEqualTo(2);
        assertThat(service.wordCount("float")).isEqualTo(2);
        assertThat(service.wordCount("INT16")).isEqualTo(1);
        assertThat(service.wordCount("UINT16")).isEqualTo(1);
        assertThat(service.wordCount("STRING")).isEqualTo(1);
    }

    @Test
    void floatEncodeDecodeRoundTrips() {
        byte[] encoded = service.encodeWriteData("3.14", "FLOAT");

        assertThat(encoded).hasSize(4);
        assertThat(Float.parseFloat(service.decodeValue(encoded, "FLOAT"))).isEqualTo(3.14f);
    }

    @Test
    void int32EncodeDecodeRoundTrips() {
        byte[] encoded = service.encodeWriteData("70000", "INT32");

        assertThat(encoded).hasSize(4);
        assertThat(service.decodeValue(encoded, "INT32")).isEqualTo("70000");
    }

    @Test
    void encodeWriteDataRejectsUnparseableValues() {
        assertThatThrownBy(() -> service.encodeWriteData("abc", "UINT16"))
                .isInstanceOf(WritePointException.class)
                .hasMessageContaining("not a valid UINT16");
        assertThatThrownBy(() -> service.encodeWriteData("x", "FLOAT"))
                .isInstanceOf(WritePointException.class)
                .hasMessageContaining("not a valid FLOAT");
    }

    @Test
    void nodeRegistrationFrameMatchesOmronW420Layout() {
        byte[] frame = service.buildNodeRegistrationFrame();

        assertThat(frame).hasSize(16);
        assertThat(new String(frame, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("FINS");
        // length counts from the command field: command(4) + error(4)
        assertThat(ByteBuffer.wrap(frame, 4, 4).order(ByteOrder.BIG_ENDIAN).getInt())
                .isEqualTo(8);
        assertThat(ByteBuffer.wrap(frame, 8, 4).order(ByteOrder.BIG_ENDIAN).getInt())
                .isEqualTo(0x00000000);
        assertThat(ByteBuffer.wrap(frame, 12, 4).order(ByteOrder.BIG_ENDIAN).getInt())
                .isEqualTo(0x00000000);
    }

    @Test
    void tcpEnvelopeCarriesMagicLengthCommandAndFinsFrame() {
        byte[] fins = {(byte) 0x80, 0x00, 0x02, 0x00, 0x02, 0x00, 0x00, 0x01, 0x00, 0x01, 0x01, 0x01};
        byte[] wrapped = service.wrapWithTcpHeader(fins);

        assertThat(wrapped).hasSize(16 + fins.length);
        assertThat(new String(wrapped, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("FINS");
        // length counts from the command field: command(4) + error(4) + fins frame
        assertThat(ByteBuffer.wrap(wrapped, 4, 4).order(ByteOrder.BIG_ENDIAN).getInt())
                .isEqualTo(8 + fins.length);
        assertThat(ByteBuffer.wrap(wrapped, 8, 4).order(ByteOrder.BIG_ENDIAN).getInt())
                .isEqualTo(0x00000002);
        assertThat(ByteBuffer.wrap(wrapped, 12, 4).order(ByteOrder.BIG_ENDIAN).getInt())
                .isEqualTo(0x00000000);
        assertThat(Arrays.copyOfRange(wrapped, 16, wrapped.length)).isEqualTo(fins);
    }

    @Test
    void fullHandshakeAndReadFlowSucceedsAgainstAFakePlc() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            AtomicReference<Throwable> plcError = new AtomicReference<>();
            CountDownLatch finished = new CountDownLatch(1);
            Thread plc = new Thread(() -> {
                try (Socket connection = server.accept()) {
                    InputStream in = connection.getInputStream();

                    // node registration request → acknowledge with assigned client node 0x0A
                    byte[] registration = readN(in, 16);
                    assertThat(registration).isEqualTo(service.buildNodeRegistrationFrame());
                    ByteBuffer ack = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
                    ack.put("FINS".getBytes(StandardCharsets.US_ASCII));
                    ack.putInt(12);
                    ack.putInt(0x00000001);
                    ack.putInt(0);
                    ack.putInt(0x0000000A);
                    connection.getOutputStream().write(ack.array());

                    // data request → FINS response frame with end code 0 and one UINT16 word
                    byte[] request = readN(in, 16 + 18);
                    assertThat(new String(request, 0, 4, StandardCharsets.US_ASCII))
                            .isEqualTo("FINS");
                    assertThat(ByteBuffer.wrap(request, 8, 4)
                                    .order(ByteOrder.BIG_ENDIAN)
                                    .getInt())
                            .isEqualTo(0x00000002);
                    // FINS header: ICF RSV GCT DNA DA1 DA2 SNA SA1 SA2 SID
                    assertThat(request[16 + 3]).isEqualTo((byte) 0x00); // DNA — local network
                    assertThat(request[16 + 4]).isEqualTo((byte) 0x02); // DA1 — destNode config default
                    assertThat(request[16 + 7]).isEqualTo((byte) 0x0A); // SA1 — handshake-assigned node

                    byte[] finsResponse = new byte[14 + 2];
                    finsResponse[0] = (byte) 0xC0; // response ICF
                    finsResponse[10] = 0x01; // MRC
                    finsResponse[11] = 0x01; // SRC
                    // end code 0x0000 at [12..13]; data = UINT16 500 (big-endian per FINS)
                    finsResponse[14] = 0x01;
                    finsResponse[15] = (byte) 0xF4;
                    ByteBuffer data =
                            ByteBuffer.allocate(16 + finsResponse.length).order(ByteOrder.BIG_ENDIAN);
                    data.put("FINS".getBytes(StandardCharsets.US_ASCII));
                    data.putInt(8 + finsResponse.length);
                    data.putInt(0x00000002);
                    data.putInt(0);
                    data.put(finsResponse);
                    connection.getOutputStream().write(data.array());
                } catch (Throwable t) {
                    plcError.set(t);
                } finally {
                    finished.countDown();
                }
            });
            plc.start();

            Map<String, AttributeBO> driverConfig = new HashMap<>();
            driverConfig.put(
                    "host",
                    AttributeBO.builder()
                            .value("127.0.0.1")
                            .type(AttributeTypeEnum.STRING)
                            .build());
            driverConfig.put(
                    "port",
                    AttributeBO.builder()
                            .value(String.valueOf(server.getLocalPort()))
                            .type(AttributeTypeEnum.INT)
                            .build());
            driverConfig.put(
                    "destNode",
                    AttributeBO.builder().value("2").type(AttributeTypeEnum.INT).build());
            Map<String, AttributeBO> pointConfig = new HashMap<>();
            pointConfig.put(
                    "memoryArea",
                    AttributeBO.builder()
                            .value("D")
                            .type(AttributeTypeEnum.STRING)
                            .build());
            pointConfig.put(
                    "address",
                    AttributeBO.builder()
                            .value("100")
                            .type(AttributeTypeEnum.INT)
                            .build());
            pointConfig.put(
                    "dataType",
                    AttributeBO.builder()
                            .value("UINT16")
                            .type(AttributeTypeEnum.STRING)
                            .build());

            DeviceBO device = new DeviceBO();
            device.setId(1L);
            PointBO point = new PointBO();
            point.setId(2L);

            ReadPointValue value = service.read(driverConfig, pointConfig, device, point);

            assertThat(value.getValue()).isEqualTo("500");
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
}
