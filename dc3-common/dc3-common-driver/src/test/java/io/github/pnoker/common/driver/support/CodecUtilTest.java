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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

class CodecUtilTest {

    @Test
    void bytesToHexEmitsTwoCharsPerByte() {
        byte[] bytes = {0x00, 0x0F, (byte) 0xA1, (byte) 0xFF};
        assertThat(CodecUtil.bytesToHex(bytes)).isEqualTo("000FA1FF");
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<CodecUtil> constructor = CodecUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void hexToBytesParsesMixedCaseAndWhitespace() {
        assertThat(CodecUtil.hexToBytes("0F a1")).containsExactly((byte) 0x0F, (byte) 0xA1);
        assertThat(CodecUtil.hexToBytes(null)).isEmpty();
    }

    @Test
    void sumChecksumMatchesFrameConvention() {
        byte[] frame = {0x68, 0x01, 0x02, 0x03};
        // 0x01 + 0x02 + 0x03 = 0x06
        assertThat(CodecUtil.sumChecksum(frame, 1, 4)).isEqualTo((byte) 0x06);
    }
}
