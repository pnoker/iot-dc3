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
package io.github.pnoker.common.agentic.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import java.util.Base64;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the provider API key cipher: roundtrip, lazy
 * migration of legacy plaintext, wrong-key and corruption failures, and the
 * derived-key fallback path.
 */
class AgenticKeyCipherTest {

    private static final String KEY_A = Base64.getEncoder().encodeToString(new byte[] {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
        31, 32
    });
    private static final String KEY_B = Base64.getEncoder().encodeToString(new byte[] {
        32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3,
        2, 1
    });

    private static AgenticProperties propertiesWithMasterKey(String masterKey) {
        AgenticProperties properties = new AgenticProperties();
        properties.setSecurityMasterKey(masterKey);
        return properties;
    }

    @Test
    void encryptDecryptRoundTripsThroughTheEnvelopeFormat() {
        AgenticKeyCipher cipher = new AgenticKeyCipher(propertiesWithMasterKey(KEY_A));
        String stored = cipher.encrypt("sk-provider-secret-123");
        assertThat(stored).startsWith(AgenticKeyCipher.CIPHERTEXT_PREFIX);
        assertThat(stored).doesNotContain("sk-provider-secret-123");
        assertThat(cipher.decrypt(stored)).isEqualTo("sk-provider-secret-123");
        assertThat(AgenticKeyCipher.isEncrypted(stored)).isTrue();
    }

    @Test
    void everyEncryptionUsesAFreshIv() {
        AgenticKeyCipher cipher = new AgenticKeyCipher(propertiesWithMasterKey(KEY_A));
        assertThat(cipher.encrypt("same-input")).isNotEqualTo(cipher.encrypt("same-input"));
    }

    @Test
    void legacyPlaintextPassesThroughDecryptForLazyMigration() {
        AgenticKeyCipher cipher = new AgenticKeyCipher(propertiesWithMasterKey(KEY_A));
        assertThat(AgenticKeyCipher.isEncrypted("legacy-plaintext-key")).isFalse();
        assertThat(cipher.decrypt("legacy-plaintext-key")).isEqualTo("legacy-plaintext-key");
    }

    @Test
    void blankKeysPassThroughUnchanged() {
        AgenticKeyCipher cipher = new AgenticKeyCipher(propertiesWithMasterKey(KEY_A));
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.encrypt("")).isEmpty();
        assertThat(cipher.decrypt(null)).isNull();
        assertThat(cipher.decrypt("")).isEmpty();
    }

    @Test
    void wrongMasterKeyFailsLoudlyInsteadOfReturningGarbage() {
        String stored = new AgenticKeyCipher(propertiesWithMasterKey(KEY_A)).encrypt("sk-provider-secret-123");
        AgenticKeyCipher otherKey = new AgenticKeyCipher(propertiesWithMasterKey(KEY_B));
        assertThatThrownBy(() -> otherKey.decrypt(stored))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be decrypted");
    }

    @Test
    void corruptedCiphertextFailsWithAClearError() {
        AgenticKeyCipher cipher = new AgenticKeyCipher(propertiesWithMasterKey(KEY_A));
        assertThatThrownBy(() -> cipher.decrypt(AgenticKeyCipher.CIPHERTEXT_PREFIX + "!!!not-base64!!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("malformed");
        assertThatThrownBy(() -> cipher.decrypt(
                        AgenticKeyCipher.CIPHERTEXT_PREFIX + Base64.getEncoder().encodeToString(new byte[] {1, 2, 3})))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("truncated");
    }

    @Test
    void derivedKeyIsStableAcrossCipherInstances() {
        // The platform-security-key fallback must derive the same key every
        // time, otherwise rows written before a restart become unreadable.
        AgenticKeyCipher first = new AgenticKeyCipher(propertiesWithMasterKey(""), () -> "platform-secret");
        AgenticKeyCipher second = new AgenticKeyCipher(propertiesWithMasterKey(""), () -> "platform-secret");
        assertThat(second.decrypt(first.encrypt("sk-derived-key"))).isEqualTo("sk-derived-key");
    }

    @Test
    void explicitMasterKeyWrongSizeFailsAtConstruction() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new AgenticKeyCipher(propertiesWithMasterKey(shortKey)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256-bit");
    }

    @Test
    void missingKeyMaterialFailsEncryptButStillReadsLegacyPlaintext() {
        Assumptions.assumeTrue(
                System.getenv("DC3_AGENTIC_MASTER_KEY") == null,
                "DC3_AGENTIC_MASTER_KEY is set in this environment; the no-material path cannot be exercised");
        AgenticKeyCipher cipher = new AgenticKeyCipher(null, () -> null);
        assertThatThrownBy(() -> cipher.encrypt("sk-some-key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires key material");
        assertThat(cipher.decrypt("legacy-row")).isEqualTo("legacy-row");
    }
}
