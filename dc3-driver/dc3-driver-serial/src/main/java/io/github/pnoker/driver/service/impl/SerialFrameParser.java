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

import io.github.pnoker.common.exception.ReadPointException;
import lombok.extern.slf4j.Slf4j;

import java.util.HexFormat;
import java.util.Objects;

/**
 * Configurable serial frame parser supporting frame headers, footers,
 * data offsets, length fields, and checksum validation (CRC16/XOR/NONE).
 * <p>
 * Frame structure: [Header][...Length...][...Data...][...Checksum...][Footer]
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
public class SerialFrameParser {

    private final byte[] frameHeader;
    private final byte[] frameFooter;
    private final int dataOffset;
    private final int dataLength;
    private final ChecksumType checksumType;

    public SerialFrameParser(String frameHeaderHex, String frameFooterHex,
                             int dataOffset, int dataLength, String checksumTypeName) {
        this.frameHeader = isBlank(frameHeaderHex) ? null : hexToBytes(frameHeaderHex);
        this.frameFooter = isBlank(frameFooterHex) ? null : hexToBytes(frameFooterHex);
        this.dataOffset = Math.max(0, dataOffset);
        this.dataLength = Math.max(0, dataLength);
        this.checksumType = parseChecksumType(checksumTypeName);
    }

    /**
     * Convert a HEX string to byte array.
     *
     * @param hex HEX string (e.g. "0103" or "01 03")
     * @return byte array
     */
    public static byte[] hexToBytes(String hex) {
        if (isBlank(hex)) {
            return new byte[0];
        }
        String cleaned = hex.replaceAll("[\\s-]", "");
        return HexFormat.of().parseHex(cleaned);
    }

    /**
     * Convert a byte array to HEX string.
     *
     * @param bytes byte array
     * @return uppercase HEX string
     */
    public static String bytesToHex(byte[] bytes) {
        if (Objects.isNull(bytes)) {
            return "";
        }
        return HexFormat.of().formatHex(bytes).toUpperCase();
    }

    /**
     * Find the first occurrence of pattern in data.
     */
    private static int indexOf(byte[] data, byte[] pattern) {
        for (int i = 0; i <= data.length - pattern.length; i++) {
            boolean found = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the last occurrence of pattern in data starting from a given offset.
     */
    private static int lastIndexOf(byte[] data, byte[] pattern, int fromIndex) {
        for (int i = data.length - pattern.length; i >= fromIndex; i--) {
            boolean found = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }

    private static boolean arrayEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static ChecksumType parseChecksumType(String name) {
        if (isBlank(name)) {
            return ChecksumType.NONE;
        }
        try {
            return ChecksumType.valueOf(name.toUpperCase().trim());
        } catch (IllegalArgumentException ignored) {
            return ChecksumType.NONE;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Parse a raw byte response and extract the data region.
     *
     * @param raw raw response bytes
     * @return extracted data bytes
     * @throws ReadPointException if frame validation fails
     */
    public byte[] parse(byte[] raw) {
        if (Objects.isNull(raw) || raw.length == 0) {
            throw new ReadPointException("Empty serial response");
        }

        int start = 0;
        int end = raw.length;

        // Locate frame header
        if (Objects.nonNull(frameHeader) && frameHeader.length > 0) {
            start = indexOf(raw, frameHeader);
            if (start < 0) {
                throw new ReadPointException("Frame header not found in serial response");
            }
            start += frameHeader.length;
        }

        // Locate frame footer (search from end of header)
        if (Objects.nonNull(frameFooter) && frameFooter.length > 0) {
            int footerIndex = lastIndexOf(raw, frameFooter, start);
            if (footerIndex < 0) {
                throw new ReadPointException("Frame footer not found in serial response");
            }
            end = footerIndex;
        }

        // Apply data offset
        int dataStart = start + dataOffset;

        // Calculate checksum region and data region
        int checksumLength = getChecksumLength();
        int dataEnd = end - checksumLength;

        if (dataEnd <= dataStart) {
            throw new ReadPointException("No data region in serial frame: start={}, end={}", dataStart, dataEnd);
        }

        // Validate checksum
        if (checksumType != ChecksumType.NONE && checksumLength > 0) {
            byte[] payload = new byte[dataEnd - start];
            System.arraycopy(raw, start, payload, 0, payload.length);
            byte[] expectedChecksum = extractChecksum(raw, dataEnd, checksumLength);
            byte[] actualChecksum = computeChecksum(payload);
            if (!arrayEquals(expectedChecksum, actualChecksum)) {
                throw new ReadPointException("Serial frame checksum mismatch: expected={}, actual={}",
                        bytesToHex(expectedChecksum), bytesToHex(actualChecksum));
            }
        }

        // Extract data region
        int finalLength = dataLength > 0 ? Math.min(dataLength, dataEnd - dataStart) : dataEnd - dataStart;
        byte[] data = new byte[finalLength];
        System.arraycopy(raw, dataStart, data, 0, finalLength);
        return data;
    }

    private int getChecksumLength() {
        return switch (checksumType) {
            case CRC16 -> 2;
            case XOR -> 1;
            case NONE -> 0;
        };
    }

    private byte[] extractChecksum(byte[] raw, int offset, int length) {
        byte[] checksum = new byte[length];
        System.arraycopy(raw, offset, checksum, 0, length);
        return checksum;
    }

    private byte[] computeChecksum(byte[] payload) {
        return switch (checksumType) {
            case CRC16 -> computeCrc16(payload);
            case XOR -> computeXor(payload);
            case NONE -> new byte[0];
        };
    }

    /**
     * Compute CRC16 (Modbus) checksum.
     */
    private byte[] computeCrc16(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return new byte[]{(byte) (crc & 0xFF), (byte) ((crc >> 8) & 0xFF)};
    }

    /**
     * Compute XOR checksum.
     */
    private byte[] computeXor(byte[] data) {
        byte xor = 0;
        for (byte b : data) {
            xor ^= b;
        }
        return new byte[]{xor};
    }

    /**
     * Checksum algorithm enumeration.
     */
    public enum ChecksumType {
        NONE,
        CRC16,
        XOR
    }
}
