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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecodeUtilTest {

    @Test
    void byteToStringDecodesUtf8() {
        byte[] bytes = "你好-dc3".getBytes(StandardCharsets.UTF_8);
        assertThat(DecodeUtil.byteToString(bytes)).isEqualTo("你好-dc3");
    }

    @Test
    void stringToByteRoundTripsUtf8() {
        String original = "iot-dc3 ✓";
        byte[] encoded = DecodeUtil.stringToByte(original);
        assertThat(DecodeUtil.byteToString(encoded)).isEqualTo(original);
    }

    @Test
    void md5IsStableForFixedInput() {
        assertThat(DecodeUtil.md5("dc3")).isEqualTo("f2925dd739df3212c39c71e9f8d64581");
    }

    @Test
    void md5WithSaltDiffersFromUnsalted() {
        String unsalted = DecodeUtil.md5("dc3");
        String salted = DecodeUtil.md5("dc3", "salty");
        assertThat(salted).isNotEqualTo(unsalted);
    }

    @Test
    void md5WithEmptySaltEqualsUnsalted() {
        assertThat(DecodeUtil.md5("dc3", "")).isEqualTo(DecodeUtil.md5("dc3"));
    }

    @Test
    void base64ByteRoundTrip() {
        byte[] payload = {0x01, 0x02, 0x03, 0x04};
        byte[] encoded = DecodeUtil.encode(payload);
        assertThat(DecodeUtil.decode(encoded)).containsExactly(payload);
    }

    @Test
    void base64StringRoundTrip() {
        String original = "iot-dc3 platform";
        byte[] encoded = DecodeUtil.encode(original);
        byte[] decoded = DecodeUtil.decode(encoded);
        assertThat(DecodeUtil.byteToString(decoded)).isEqualTo(original);
    }

    @Test
    void base64DecodeFromEncodedString() {
        String original = "encoded-payload";
        byte[] encoded = DecodeUtil.encode(original);
        String encodedAsString = DecodeUtil.byteToString(encoded);
        assertThat(DecodeUtil.byteToString(DecodeUtil.decode(encodedAsString)))
                .isEqualTo(original);
    }

    @Test
    void hexEncodingRoundTripsForAscii() {
        String original = "abc";
        String hex = DecodeUtil.enHexCode(original);
        assertThat(hex).isEqualTo("616263");
        assertThat(DecodeUtil.deHexCode(hex)).containsExactly('a', 'b', 'c');
    }

    @Test
    void hexEncodingRoundTripsForBinary() {
        String original = new String(new byte[]{(byte) 0xff, 0x00, 0x10}, StandardCharsets.ISO_8859_1);
        String hex = DecodeUtil.enHexCode(new String(original.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertThat(hex).isNotBlank();
    }

    @Test
    void deHexCodeRejectsOddLengthInput() {
        assertThatThrownBy(() -> DecodeUtil.deHexCode("abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void md5RejectsNullInput() {
        assertThatThrownBy(() -> DecodeUtil.md5(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<DecodeUtil> constructor = DecodeUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
