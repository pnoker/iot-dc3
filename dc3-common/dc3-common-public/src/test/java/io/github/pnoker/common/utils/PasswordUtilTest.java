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

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilTest {

    @Test
    void encodeProducesBcryptHash() {
        String hash = PasswordUtil.encode("password123");
        assertThat(hash).startsWith("$2a$");
    }

    @Test
    void verifyMatchesEncodedPassword() {
        String raw = "testPassword";
        String hash = PasswordUtil.encode(raw);
        assertThat(PasswordUtil.verify(raw, hash)).isTrue();
    }

    @Test
    void verifyRejectsWrongPassword() {
        String hash = PasswordUtil.encode("correct");
        assertThat(PasswordUtil.verify("wrong", hash)).isFalse();
    }

    @Test
    void encodeProducesUniqueHashesPerCall() {
        String raw = "samePassword";
        String hash1 = PasswordUtil.encode(raw);
        String hash2 = PasswordUtil.encode(raw);
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(PasswordUtil.verify(raw, hash1)).isTrue();
        assertThat(PasswordUtil.verify(raw, hash2)).isTrue();
    }

    @Test
    void fullFlowMd5PrehashToBcrypt() {
        String rawPassword = "dc3dc3dc3";
        String prehashed = DecodeUtil.md5(rawPassword);
        String stored = PasswordUtil.encode(prehashed);
        assertThat(PasswordUtil.verify(prehashed, stored)).isTrue();
    }
}
