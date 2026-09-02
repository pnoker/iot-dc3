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

import io.github.pnoker.common.exception.ConnectorException;

/**
 * DL/T 645-2007 frame codec.
 * <p>
 * Builds master request frames and parses meter response frames. Frames use the
 * standard layout {@code 68 A0..A5 68 C L DATA CS 16} where the checksum is the
 * low byte of the running sum from the first {@code 0x68} through the final data
 * byte. Meter-to-master data fields are transmitted with each byte offset by
 * {@code +0x33}, which is reversed on parse.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
public final class Dlt645Frame {

    /**
     * Frame start byte.
     */
    public static final byte START = (byte) 0x68;
    /**
     * Frame end byte.
     */
    public static final byte END = (byte) 0x16;
    /**
     * Read data request control code.
     */
    public static final byte CONTROL_READ = 0x11;
    /**
     * Write data request control code.
     */
    public static final byte CONTROL_WRITE = 0x14;
    /**
     * Read response control code (no follow-up frame).
     */
    public static final byte CONTROL_READ_RESPONSE = (byte) 0x91;
    /**
     * Read response control code (follow-up frame).
     */
    public static final byte CONTROL_READ_RESPONSE_MORE = (byte) 0xB1;
    /**
     * Write success response control code.
     */
    public static final byte CONTROL_WRITE_RESPONSE = (byte) 0x94;
    /**
     * Value each response data byte is offset by.
     */
    public static final byte RESPONSE_OFFSET = 0x33;

    private Dlt645Frame() {}

    /**
     * Encode a 12-digit BCD address string into its 6-byte form.
     *
     * @param address 12-character decimal address, e.g. {@code 000000000001}
     * @return 6-byte compressed BCD address
     */
    public static byte[] encodeAddress(String address) {
        if (address == null || address.length() != 12 || !address.matches("\\d{12}")) {
            throw new ConnectorException("Invalid DL/T 645 meter address: {}", address);
        }
        byte[] result = new byte[6];
        for (int i = 0; i < 6; i++) {
            int high = address.charAt(i * 2) - '0';
            int low = address.charAt(i * 2 + 1) - '0';
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    /**
     * Build a read-data request frame.
     *
     * @param address 6-byte meter address
     * @param di      four-byte data identifier (DI0..DI3)
     * @return complete request frame
     */
    public static byte[] buildReadRequest(byte[] address, int[] di) {
        byte[] data = new byte[] {(byte) di[0], (byte) di[1], (byte) di[2], (byte) di[3]};
        return build(address, CONTROL_READ, data);
    }

    /**
     * Build a write-data request frame.
     *
     * @param address      6-byte meter address
     * @param password     4-byte password
     * @param operatorCode 4-byte operator code
     * @param di           four-byte data identifier (DI0..DI3)
     * @param payload      value bytes to write (raw, not yet +0x33)
     * @return complete request frame
     */
    public static byte[] buildWriteRequest(
            byte[] address, byte[] password, byte[] operatorCode, int[] di, byte[] payload) {
        byte[] data = new byte[4 + 4 + 4 + payload.length];
        System.arraycopy(password, 0, data, 0, 4);
        System.arraycopy(operatorCode, 0, data, 4, 4);
        data[8] = (byte) di[0];
        data[9] = (byte) di[1];
        data[10] = (byte) di[2];
        data[11] = (byte) di[3];
        for (int i = 0; i < payload.length; i++) {
            data[12 + i] = (byte) (payload[i] + RESPONSE_OFFSET);
        }
        return build(address, CONTROL_WRITE, data);
    }

    private static byte[] build(byte[] address, byte control, byte[] data) {
        byte[] frame = new byte[12 + data.length];
        int pos = 0;
        frame[pos++] = START;
        for (byte b : address) {
            frame[pos++] = b;
        }
        frame[pos++] = START;
        frame[pos++] = control;
        frame[pos++] = (byte) data.length;
        for (byte b : data) {
            frame[pos++] = b;
        }
        frame[pos++] = checksum(frame, pos);
        frame[pos] = END;
        return frame;
    }

    /**
     * Parse a response frame, validating structure and checksum.
     *
     * @param response raw response bytes
     * @return data field with the {@code +0x33} offset reversed
     */
    public static byte[] parse(byte[] response) {
        if (response == null || response.length < 13) {
            throw new ConnectorException(
                    "DL/T 645 response too short: {} bytes", response == null ? 0 : response.length);
        }
        if ((response[0] & 0xFF) != (START & 0xFF) || (response[response.length - 1] & 0xFF) != (END & 0xFF)) {
            throw new ConnectorException("DL/T 645 response has invalid frame delimiters");
        }
        if (!verifyChecksum(response)) {
            throw new ConnectorException("DL/T 645 response checksum mismatch");
        }
        int length = response[9] & 0xFF;
        byte[] data = new byte[length - 4];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (response[14 + i] - RESPONSE_OFFSET);
        }
        return data;
    }

    /**
     * Read the control code from a response frame.
     *
     * @param response raw response bytes
     * @return control code byte
     */
    public static byte control(byte[] response) {
        if (response == null || response.length < 10) {
            throw new ConnectorException("DL/T 645 response too short to read control code");
        }
        return response[8];
    }

    /**
     * Verify the running-sum checksum over the whole frame.
     *
     * @param frame frame including trailing checksum and end byte
     * @return {@code true} when the checksum matches
     */
    public static boolean verifyChecksum(byte[] frame) {
        int length = frame.length;
        return (checksum(frame, length - 2) & 0xFF) == (frame[length - 2] & 0xFF);
    }

    private static byte checksum(byte[] bytes, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += bytes[i] & 0xFF;
        }
        return (byte) (sum & 0xFF);
    }
}
