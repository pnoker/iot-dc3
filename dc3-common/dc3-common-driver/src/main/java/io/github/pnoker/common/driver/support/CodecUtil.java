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
package io.github.pnoker.common.driver.support;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class providing various utility methods for encoding, decoding, and byte
 * manipulation operations including Base64, BCD, Hex, and ASCII conversions.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
public class CodecUtil {

    private CodecUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Converts bytes to a hexadecimal string.
     *
     * @param bytes byte array
     * @return hexadecimal string
     */
    public static String bytesToHex(byte[] bytes) {
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }

    /**
     * Parses a hexadecimal string into bytes, tolerating embedded whitespace and
     * returning an empty array for blank input. Both letter cases are accepted.
     *
     * @param hex hexadecimal string, possibly separated by spaces
     * @return decoded bytes
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isBlank()) {
            return new byte[0];
        }
        return HexFormat.of().parseHex(hex.replaceAll("\\s+", ""));
    }

    /**
     * Sum checksum over {@code bytes[from..to)} truncated to one byte - the
     * frame checksum shared by the MBus and DL/T645 wire formats.
     *
     * @param bytes frame bytes
     * @param from start index, inclusive
     * @param to end index, exclusive
     * @return low byte of the arithmetic sum
     */
    public static byte sumChecksum(byte[] bytes, int from, int to) {
        int sum = 0;
        for (int i = from; i < to; i++) {
            sum += bytes[i] & 0xFF;
        }
        return (byte) (sum & 0xFF);
    }
}
