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

import io.github.pnoker.common.entity.auth.Keys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyUtilTest {

    private static final String SALT = "0123456789abcdef0123456789abcdef";

    private static final String OTHER_SALT = "fedcba9876543210fedcba9876543210";

    @BeforeAll
    static void setUpSecurityKey() {
        System.setProperty("dc3.security.key", "test-security-key-for-junit");
    }

    @AfterAll
    static void tearDownSecurityKey() {
        System.clearProperty("dc3.security.key");
    }

    @Test
    void aesEncryptDecryptRoundTrip() throws Exception {
        Keys.Aes key = KeyUtil.genAesKey();
        String plaintext = "iot-dc3 secret payload";
        String encrypted = KeyUtil.encryptAes(plaintext, key.getPrivateKey());
        String decrypted = KeyUtil.decryptAes(encrypted, key.getPrivateKey());
        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(encrypted).isNotEqualTo(plaintext);
    }

    @Test
    void aesEncryptionRejectsBlankPrivateKey() {
        assertThatThrownBy(() -> KeyUtil.encryptAes("payload", ""))
                .isInstanceOf(Exception.class);
    }

    @Test
    void aesGenerationProducesUniqueKeys() throws Exception {
        Keys.Aes a = KeyUtil.genAesKey();
        Keys.Aes b = KeyUtil.genAesKey();
        assertThat(a.getPrivateKey()).isNotEqualTo(b.getPrivateKey());
    }

    @Test
    void rsaEncryptDecryptRoundTrip() throws Exception {
        Keys.Rsa key = KeyUtil.genRsaKey();
        String plaintext = "iot-dc3 rsa payload";
        String encrypted = KeyUtil.encryptRsa(plaintext, key.getPublicKey());
        String decrypted = KeyUtil.decryptRsa(encrypted, key.getPrivateKey());
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void rsaGenerationProducesPublicAndPrivateMaterial() throws Exception {
        Keys.Rsa key = KeyUtil.genRsaKey();
        assertThat(key.getPublicKey()).isNotBlank();
        assertThat(key.getPrivateKey()).isNotBlank();
        assertThat(key.getPublicKey()).isNotEqualTo(key.getPrivateKey());
    }

    @Test
    void jwtRoundTripsForValidIssuerAndSubject() {
        String token = KeyUtil.generateToken("alice", SALT, 100L);
        Claims claims = KeyUtil.parserToken("alice", SALT, token, 100L);
        assertThat(claims.getSubject()).contains("alice");
        assertThat(claims.getIssuer()).contains("100");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void jwtParsingRejectsTokenSignedWithDifferentSalt() {
        String token = KeyUtil.generateToken("alice", SALT, 100L);
        assertThatThrownBy(() -> KeyUtil.parserToken("alice", OTHER_SALT, token, 100L))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void jwtParsingRejectsTokenForDifferentSubject() {
        String token = KeyUtil.generateToken("alice", SALT, 100L);
        assertThatThrownBy(() -> KeyUtil.parserToken("bob", SALT, token, 100L))
                .isInstanceOf(io.jsonwebtoken.IncorrectClaimException.class);
    }

    @Test
    void jwtParsingRejectsTokenForDifferentTenant() {
        String token = KeyUtil.generateToken("alice", SALT, 100L);
        assertThatThrownBy(() -> KeyUtil.parserToken("alice", SALT, token, 200L))
                .isInstanceOf(io.jsonwebtoken.IncorrectClaimException.class);
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<KeyUtil> constructor = KeyUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
