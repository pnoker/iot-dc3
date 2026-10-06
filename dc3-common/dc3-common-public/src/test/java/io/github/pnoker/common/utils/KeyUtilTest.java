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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.pnoker.common.entity.auth.Keys;
import io.jsonwebtoken.Claims;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class KeyUtilTest {

    @BeforeAll
    static void setUpSecurityKey() {
        System.setProperty("dc3.security.key", "test-security-key-for-junit-0123456789abcdef");
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
        assertThatThrownBy(() -> KeyUtil.encryptAes("payload", "")).isInstanceOf(Exception.class);
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
        String token = KeyUtil.generateToken("alice", 100L);
        Claims claims = KeyUtil.parserToken("alice", token, 100L);
        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.getIssuer()).isEqualTo("dc3-auth");
        assertThat(claims.get("tenantId", String.class)).isEqualTo("100");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void jwtPayloadNeverDisclosesTheConfiguredSecurityKey() {
        // getSecurityKey prefers DC3_SECURITY_KEY over the system property, so this guard is only meaningful when the
        // environment variable is absent; otherwise the property override below would not reach the signer.
        assumeTrue(
                System.getenv("DC3_SECURITY_KEY") == null,
                "DC3_SECURITY_KEY env var takes precedence over the property");
        String previousKey = System.getProperty("dc3.security.key");
        String uniqueKey = "guard-unique-key-0123456789abcdefghijklmnopqrstuv";
        System.setProperty("dc3.security.key", uniqueKey);
        try {
            String token = KeyUtil.generateToken("alice", 100L);
            String payload = new String(
                    java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                    java.nio.charset.StandardCharsets.UTF_8);
            // Sanity-check the decode targeted the payload segment before asserting the absence.
            assertThat(payload).contains("alice");
            assertThat(payload).doesNotContain(uniqueKey);
        } finally {
            if (previousKey == null) System.clearProperty("dc3.security.key");
            else System.setProperty("dc3.security.key", previousKey);
        }
    }

    @Test
    void jwtParsingRejectsTokenForDifferentSubject() {
        String token = KeyUtil.generateToken("alice", 100L);
        assertThatThrownBy(() -> KeyUtil.parserToken("bob", token, 100L))
                .isInstanceOf(io.jsonwebtoken.IncorrectClaimException.class);
    }

    @Test
    void jwtParsingRejectsTokenForDifferentTenant() {
        String token = KeyUtil.generateToken("alice", 100L);
        assertThatThrownBy(() -> KeyUtil.parserToken("alice", token, 200L))
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
