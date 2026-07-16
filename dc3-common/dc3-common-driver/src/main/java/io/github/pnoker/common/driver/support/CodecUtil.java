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
import io.github.pnoker.common.utils.DecodeUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class providing various utility methods for encoding, decoding, and byte
 * manipulation operations including Base64, BCD, Hex, and ASCII conversions.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class CodecUtil {

    private CodecUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Encodes the provided string into a Base64 string.
     *
     * @param content input string
     * @return Base64-encoded string
     */
    public static String base64Encode(String content) {
        byte[] bytes = DecodeUtil.stringToByte(content);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Decodes a Base64 string into its original form.
     *
     * @param content Base64 string
     * @return decoded string
     */
    public static String base64Decode(String content) {
        byte[] bytes = DecodeUtil.stringToByte(content);
        return new String(Base64.getDecoder().decode(bytes), StandardCharsets.UTF_8);
    }

    /**
     * Converts BCD bytes to a decimal string.
     *
     * @param bytes BCD bytes
     * @return decimal string
     */
    public static String bcdBytesToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            sb.append((byte) ((aByte & 0xf0) >>> 4));
            sb.append((byte) (aByte & 0x0f));
        }
        return sb.substring(0, 1).equalsIgnoreCase("0") ? sb.substring(1) : sb.toString();
    }

    /**
     * Converts a decimal string to BCD bytes.
     *
     * @param decimalString decimal string
     * @return BCD bytes
     */
    public static byte[] strToBcdBytes(String decimalString) {
        int length = decimalString.length();
        int mod = length % 2;
        if (mod != 0) {
            decimalString = "0" + decimalString;
            length = decimalString.length();
        }
        if (length >= 2) {
            length = length / 2;
        }
        byte[] bcdBytes = new byte[length];
        byte[] decimalBytes = DecodeUtil.stringToByte(decimalString);
        int j;
        int k;
        for (int i = 0; i < decimalString.length() / 2; i++) {
            if ((decimalBytes[2 * i] >= '0') && (decimalBytes[2 * i] <= '9')) {
                j = decimalBytes[2 * i] - '0';
            } else if ((decimalBytes[2 * i] >= 'a') && (decimalBytes[2 * i] <= 'z')) {
                j = decimalBytes[2 * i] - 'a' + 0x0a;
            } else {
                j = decimalBytes[2 * i] - 'A' + 0x0a;
            }
            if ((decimalBytes[2 * i + 1] >= '0') && (decimalBytes[2 * i + 1] <= '9')) {
                k = decimalBytes[2 * i + 1] - '0';
            } else if ((decimalBytes[2 * i + 1] >= 'a') && (decimalBytes[2 * i + 1] <= 'z')) {
                k = decimalBytes[2 * i + 1] - 'a' + 0x0a;
            } else {
                k = decimalBytes[2 * i + 1] - 'A' + 0x0a;
            }
            bcdBytes[i] = (byte) ((j << 4) + k);
        }
        return bcdBytes;
    }

    /**
     * Converts bytes to a hexadecimal string.
     *
     * @param bytes byte array
     * @return hexadecimal string
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(aByte & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Converts a byte array to a big-endian integer.
     *
     * @param bytes byte array
     * @return integer value
     */
    public static int bytesToInt(byte[] bytes) {
        byte[] temp = new byte[4];
        int length = bytes.length;
        System.arraycopy(bytes, 0, temp, 0, length);
        for (int i = length; i < 4; i++) {
            temp[i] = 0x00;
        }
        int int1 = temp[3] & 0xff;
        int int2 = (temp[2] & 0xff) << 8;
        int int3 = (temp[1] & 0xff) << 16;
        int int4 = (temp[0] & 0xff) << 24;

        return int1 | int2 | int3 | int4;
    }

    /**
     * Converts a byte array to a little-endian integer.
     *
     * @param bytes byte array
     * @return integer value
     */
    public static int bytesToIntLE(byte[] bytes) {
        byte[] temp = new byte[4];
        int length = bytes.length;
        System.arraycopy(bytes, 0, temp, 0, length);
        for (int i = length; i < 4; i++) {
            temp[i] = 0x00;
        }
        int int1 = temp[0] & 0xff;
        int int2 = (temp[1] & 0xff) << 8;
        int int3 = (temp[2] & 0xff) << 16;
        int int4 = (temp[3] & 0xff) << 24;
        return int1 | int2 | int3 | int4;
    }

    /**
     * Converts bytes to an ISO-8859-1 string.
     *
     * @param bytes byte array
     * @return decoded string
     */
    public static String bytesToAscii(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    /**
     * Reverses the order of the supplied bytes.
     *
     * @param bytes byte array
     * @return reversed byte array
     */
    public static byte[] byteReverse(byte[] bytes) {
        int length = bytes.length;
        byte[] reverse = new byte[length];
        for (int i = 0; i < length; i++) {
            reverse[length - 1 - i] = bytes[i];
        }
        return reverse;
    }

    /**
     * Merges multiple byte arrays into a single array.
     *
     * @param bytes byte arrays
     * @return merged byte array
     */
    public static byte[] mergerBytes(byte[]... bytes) {
        int lengthByte = 0;
        for (byte[] value : bytes) {
            lengthByte += value.length;
        }
        byte[] allByte = new byte[lengthByte];
        int countLength = 0;
        for (byte[] b : bytes) {
            System.arraycopy(b, 0, allByte, countLength, b.length);
            countLength += b.length;
        }
        return allByte;
    }

    /**
     * Calculates the XOR checksum of the supplied byte arrays.
     *
     * @param bytes byte arrays
     * @return XOR checksum
     */
    public static byte xorBytes(byte[]... bytes) {
        byte xor = 0x00;
        for (byte[] value : bytes) {
            for (byte b : value) {
                xor ^= b;
            }
        }
        return xor;
    }

    /**
     * Calculates the additive checksum of the supplied byte arrays.
     *
     * @param bytes byte arrays
     * @return additive checksum
     */
    public static byte sumBytes(byte[]... bytes) {
        byte xor = 0x00;
        for (byte[] value : bytes) {
            for (byte b : value) {
                xor += b;
            }
        }
        return xor;
    }

}
