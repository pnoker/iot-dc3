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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodecUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {"a", "iot-dc3", "你好-dc3", "1234567890ABCDEF"})
    void base64EncodeDecodeRoundTrip(String original) {
        String encoded = CodecUtil.base64Encode(original);
        assertThat(CodecUtil.base64Decode(encoded)).isEqualTo(original);
    }

    @Test
    void bcdRoundTripPreservesEvenLengthDecimal() {
        String original = "1234";
        byte[] bcd = CodecUtil.strToBcdBytes(original);
        assertThat(bcd).hasSize(2);
        assertThat(CodecUtil.bcdBytesToString(bcd)).isEqualTo(original);
    }

    @Test
    void bcdLeadingZeroIsTrimmedForOddLengthInput() {
        // strToBcdBytes left-pads odd-length input with '0'; bcdBytesToString
        // trims a single leading zero. Round-trip preserves the original digits.
        byte[] bcd = CodecUtil.strToBcdBytes("123");
        assertThat(CodecUtil.bcdBytesToString(bcd)).isEqualTo("123");
    }

    @Test
    void bytesToHexEmitsTwoCharsPerByte() {
        byte[] bytes = {0x00, 0x0F, (byte) 0xA1, (byte) 0xFF};
        assertThat(CodecUtil.bytesToHex(bytes)).isEqualTo("000fa1ff");
    }

    @Test
    void bytesToIntDecodesBigEndian() {
        // 0x01020304 in big-endian (most significant byte first)
        byte[] bytes = {0x01, 0x02, 0x03, 0x04};
        assertThat(CodecUtil.bytesToInt(bytes)).isEqualTo(0x01020304);
    }

    @Test
    void bytesToIntLeDecodesLittleEndian() {
        // 0x04030201 little-endian -> source bytes {01,02,03,04}
        byte[] bytes = {0x01, 0x02, 0x03, 0x04};
        assertThat(CodecUtil.bytesToIntLE(bytes)).isEqualTo(0x04030201);
    }

    @Test
    void bytesToIntPadsShorterArrays() {
        byte[] bytes = {0x01, 0x02};
        assertThat(CodecUtil.bytesToInt(bytes)).isEqualTo(0x01020000);
    }

    @Test
    void bytesToAsciiUsesIso88591() {
        byte[] bytes = "hello".getBytes();
        assertThat(CodecUtil.bytesToAscii(bytes)).isEqualTo("hello");
    }

    @Test
    void byteReverseInvertsOrder() {
        byte[] reversed = CodecUtil.byteReverse(new byte[]{1, 2, 3, 4});
        assertThat(reversed).containsExactly(4, 3, 2, 1);
    }

    @Test
    void byteReverseHandlesEmptyArray() {
        assertThat(CodecUtil.byteReverse(new byte[]{})).isEmpty();
    }

    @Test
    void mergerBytesConcatenatesArraysInOrder() {
        byte[] merged = CodecUtil.mergerBytes(new byte[]{1, 2}, new byte[]{3}, new byte[]{4, 5});
        assertThat(merged).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void mergerBytesReturnsEmptyArrayForNoInputs() {
        assertThat(CodecUtil.mergerBytes()).isEmpty();
    }

    @Test
    void xorBytesComputesCumulativeXor() {
        byte result = CodecUtil.xorBytes(new byte[]{0x01, 0x02}, new byte[]{0x03});
        assertThat(result).isEqualTo((byte) (0x01 ^ 0x02 ^ 0x03));
    }

    @Test
    void sumBytesComputesAdditiveChecksumAcrossArrays() {
        byte result = CodecUtil.sumBytes(new byte[]{0x01, 0x02}, new byte[]{0x03, 0x04});
        assertThat(result).isEqualTo((byte) (0x01 + 0x02 + 0x03 + 0x04));
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<CodecUtil> constructor = CodecUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
