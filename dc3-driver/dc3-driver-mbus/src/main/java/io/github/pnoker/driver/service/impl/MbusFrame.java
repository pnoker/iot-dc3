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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * M-Bus (Meter-Bus, IEC 870-5) frame codec.
 * <p>
 * Builds master request frames and parses slave response frames, including the
 * DIF/VIF data-record encoding used by heat, water, and gas meters.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
public final class MbusFrame {

    /**
     * Long frame start byte.
     */
    public static final byte START = (byte) 0x68;
    /**
     * Short frame start byte.
     */
    public static final byte SHORT_START = 0x10;
    /**
     * Frame end byte.
     */
    public static final byte END = (byte) 0x16;
    /**
     * SND_UD2 control code (master request for user data).
     */
    public static final byte CONTROL_SND_UD2 = 0x5B;
    /**
     * RSP_UD control code (slave user-data response).
     */
    public static final byte CONTROL_RSP_UD = 0x08;
    /**
     * REQ_UD2 control-information code.
     */
    public static final byte CI_REQ_UD2 = 0x7A;
    /**
     * SND_UD control code (master send user data).
     */
    public static final byte CONTROL_SND_UD = 0x53;
    /**
     * SND_UD control-information code.
     */
    public static final byte CI_SND_UD = 0x51;
    /**
     * Single-byte ACK response.
     */
    public static final byte ACK = (byte) 0xE5;
    /**
     * RSP_UD control-information code.
     */
    public static final byte CI_RSP_UD = 0x72;

    private MbusFrame() {
    }

    /**
     * Build a REQ_UD2 (request user data) long frame for the given primary address.
     *
     * @param address primary address (0..250)
     * @return complete request frame
     */
    public static byte[] buildReqUd2(int address) {
        byte[] frame = new byte[9];
        frame[0] = START;
        frame[1] = 0x03;
        frame[2] = 0x03;
        frame[3] = START;
        frame[4] = CONTROL_SND_UD2;
        frame[5] = (byte) address;
        frame[6] = CI_REQ_UD2;
        frame[7] = checksum(frame, 4, 7);
        frame[8] = END;
        return frame;
    }

    /**
     * Build a SND_NKE (initialization) short frame for the given primary address.
     *
     * @param address primary address (0..250)
     * @return complete short frame
     */
    public static byte[] buildSndNke(int address) {
        byte[] frame = new byte[5];
        frame[0] = SHORT_START;
        frame[1] = 0x40;
        frame[2] = (byte) address;
        frame[3] = checksum(frame, 1, 3);
        frame[4] = END;
        return frame;
    }

    /**
     * Build a SND_UD (send user data) long frame for the given primary address.
     *
     * @param address primary address (0..250)
     * @param payload user-data bytes to write
     * @return complete request frame
     */
    public static byte[] buildSndUd(int address, byte[] payload) {
        int length = 3 + payload.length;
        byte[] frame = new byte[9 + payload.length];
        frame[0] = START;
        frame[1] = (byte) length;
        frame[2] = (byte) length;
        frame[3] = START;
        frame[4] = CONTROL_SND_UD;
        frame[5] = (byte) address;
        frame[6] = CI_SND_UD;
        System.arraycopy(payload, 0, frame, 7, payload.length);
        frame[7 + payload.length] = checksum(frame, 4, 7 + payload.length);
        frame[8 + payload.length] = END;
        return frame;
    }

    /**
     * Parse a response frame, validating delimiters and checksum, and return the
     * user-data field (bytes after the CI field).
     *
     * @param response raw response bytes
     * @return user-data bytes, possibly empty
     */
    public static byte[] parse(byte[] response) {
        if (response == null || response.length < 9) {
            throw new ConnectorException("M-Bus response too short: {} bytes", response == null ? 0 : response.length);
        }
        if ((response[0] & 0xFF) != (START & 0xFF) || (response[response.length - 1] & 0xFF) != (END & 0xFF)) {
            throw new ConnectorException("M-Bus response has invalid frame delimiters");
        }
        int length = response[1] & 0xFF;
        if (!verifyChecksum(response)) {
            throw new ConnectorException("M-Bus response checksum mismatch");
        }
        int dataLength = length - 3;
        if (dataLength <= 0) {
            return new byte[0];
        }
        return Arrays.copyOfRange(response, 7, 7 + dataLength);
    }

    /**
     * Read the control code from a response frame.
     *
     * @param response raw response bytes
     * @return control code byte
     */
    public static byte control(byte[] response) {
        if (response == null || response.length < 5) {
            throw new ConnectorException("M-Bus response too short to read control code");
        }
        return response[4];
    }

    /**
     * Verify the running-sum checksum over the control-to-data region.
     *
     * @param frame frame including trailing checksum and end byte
     * @return {@code true} when the checksum matches
     */
    public static boolean verifyChecksum(byte[] frame) {
        int length = frame[1] & 0xFF;
        int csIndex = 4 + length;
        if (frame.length < csIndex + 2) {
            return false;
        }
        return (checksum(frame, 4, csIndex) & 0xFF) == (frame[csIndex] & 0xFF);
    }

    private static byte checksum(byte[] bytes, int from, int to) {
        int sum = 0;
        for (int i = from; i < to; i++) {
            sum += bytes[i] & 0xFF;
        }
        return (byte) (sum & 0xFF);
    }

    /**
     * Decode the user-data field into a list of DIF/VIF data records.
     * <p>
     * Handles the common single-byte DIF and VIF case plus DIFE/VIFE extension bytes;
     * manufacturer-specific and variable-length records are skipped conservatively.
     * </p>
     *
     * @param data user-data bytes
     * @return decoded records
     */
    public static List<MbusRecord> parseRecords(byte[] data) {
        List<MbusRecord> records = new ArrayList<>();
        if (data == null) {
            return records;
        }
        int i = 0;
        while (i < data.length) {
            int dif = data[i] & 0xFF;
            if (dif == 0x2F || dif == 0x1F) {
                break;
            }
            int cursor = i + 1;
            if ((dif & 0x40) != 0) {
                cursor++; // skip DIFE extension byte
            }
            if (cursor >= data.length) {
                break;
            }
            int vif = data[cursor] & 0xFF;
            cursor++;
            if ((vif & 0x80) != 0 && cursor < data.length) {
                cursor++; // skip VIFE extension byte
            }
            int len = difLength(dif & 0x0F);
            if (cursor + len > data.length) {
                break;
            }
            byte[] valueBytes = Arrays.copyOfRange(data, cursor, cursor + len);
            records.add(new MbusRecord(dif, vif, valueBytes));
            i = cursor + len;
        }
        return records;
    }

    /**
     * Map a DIF low-nibble code to its value byte length.
     */
    private static int difLength(int code) {
        return switch (code) {
            case 1, 9 -> 1;
            case 2, 10 -> 2;
            case 3, 11 -> 3;
            case 4, 5, 12 -> 4;
            case 6 -> 6;
            case 7, 14 -> 8;
            default -> 0;
        };
    }

    /**
     * Decode a record value according to the requested format.
     * <p>
     * {@code AUTO} selects BCD for BCD-coded DIFs, IEEE-754 for real, and integer
     * otherwise.
     * </p>
     *
     * @param record decoded record
     * @param format HEX, BCD, INT, FLOAT, or AUTO
     * @return formatted value string
     */
    public static String decodeValue(MbusRecord record, String format) {
        byte[] bytes = record.getValueBytes();
        int code = record.getDif() & 0x0F;
        String upper = format == null ? "AUTO" : format.toUpperCase();
        if ("AUTO".equals(upper)) {
            if (code == 9 || code == 10 || code == 11 || code == 12 || code == 14) {
                upper = "BCD";
            } else if (code == 5) {
                upper = "FLOAT";
            } else {
                upper = "INT";
            }
        }
        return switch (upper) {
            case "BCD" -> toBcdString(bytes);
            case "INT" -> String.valueOf(toInt(bytes));
            case "FLOAT" -> String.valueOf(toFloat(bytes));
            default -> toHexString(bytes);
        };
    }

    private static String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private static String toBcdString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        boolean started = false;
        for (byte b : data) {
            int high = (b >> 4) & 0x0F;
            int low = b & 0x0F;
            if (high != 0) {
                started = true;
            }
            if (started) {
                sb.append((char) ('0' + high));
            }
            if (started || low != 0) {
                sb.append((char) ('0' + low));
            }
            if (!started && low != 0) {
                started = true;
            }
        }
        return sb.length() == 0 ? "0" : sb.toString();
    }

    private static long toInt(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        return switch (data.length) {
            case 1 -> bb.get() & 0xFF;
            case 2 -> bb.getShort() & 0xFFFF;
            case 4 -> bb.getInt();
            case 8 -> bb.getLong();
            default -> new BigInteger(data).longValue();
        };
    }

    private static double toFloat(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        return data.length == 4 ? bb.getFloat() : bb.getDouble();
    }
}
