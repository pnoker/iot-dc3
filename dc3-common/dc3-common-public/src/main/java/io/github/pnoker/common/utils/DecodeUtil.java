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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Encoding/decoding utility for Base64, hex, MD5, and UTF-8.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class DecodeUtil {

    private DecodeUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Convert byte array to string using UTF-8 encoding
     *
     * @param bytes Byte array to convert
     * @return String representation of byte array
     */
    public static String byteToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Convert string to byte array using UTF-8 encoding
     *
     * @param content String to convert
     * @return Byte array representation of string
     */
    public static byte[] stringToByte(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate MD5 hash of content
     *
     * @param content String to hash
     * @return MD5 hash string
     */
    public static String md5(String content) {
        return DigestUtils.md5Hex(content);
    }

    /**
     * Generate MD5 hash of content with salt
     *
     * @param content String to hash
     * @param salt    Salt value for additional security
     * @return MD5 hash string
     */
    public static String md5(String content, String salt) {
        return md5(content + salt);
    }

    /**
     * Encode byte array using Base64 encoding
     *
     * @param bytes Byte array to encode
     * @return Base64 encoded byte array
     */
    public static byte[] encode(byte[] bytes) {
        return Base64.getEncoder().encode(bytes);
    }

    /**
     * Encode string using Base64 encoding
     *
     * @param content String to encode
     * @return Base64 encoded byte array
     */
    public static byte[] encode(String content) {
        return encode(stringToByte(content));
    }

    /**
     * Decode Base64 encoded byte array Must be used with encode method
     *
     * @param bytes Base64 encoded byte array
     * @return Decoded byte array
     */
    public static byte[] decode(byte[] bytes) {
        return Base64.getDecoder().decode(bytes);
    }

    /**
     * Decode Base64 encoded string Must be used with encode method
     *
     * @param content Base64 encoded string
     * @return Decoded byte array
     */
    public static byte[] decode(String content) {
        return decode(stringToByte(content));
    }

    /**
     * Encode string to hexadecimal representation
     *
     * @param content String to encode
     * @return Hexadecimal string representation
     */
    public static String enHexCode(String content) {
        return HexFormat.of().formatHex((stringToByte(content)));
    }

    /**
     * Decode hexadecimal string to byte array Must be used with enHexCode method
     *
     * @param content Hexadecimal string to decode
     * @return Decoded byte array
     */
    public static byte[] deHexCode(String content) {
        return HexFormat.of().parseHex(content);
    }

}
