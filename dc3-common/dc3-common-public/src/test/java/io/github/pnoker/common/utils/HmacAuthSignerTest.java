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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class HmacAuthSignerTest {

    private static final String SECRET = "test-shared-secret-do-not-use-in-prod";

    @Test
    void enabledStateReflectsSecret() {
        assertThat(new HmacAuthSigner(SECRET).isEnabled()).isTrue();
        assertThat(new HmacAuthSigner(null).isEnabled()).isFalse();
        assertThat(new HmacAuthSigner("").isEnabled()).isFalse();
        assertThat(new HmacAuthSigner("   ").isEnabled()).isFalse();
    }

    @Test
    void signProducesStableHexForFixedSecretAndPayload() {
        HmacAuthSigner signer = new HmacAuthSigner(SECRET);
        String first = signer.sign("payload");
        String second = signer.sign("payload");
        assertThat(first).isEqualTo(second).matches("[0-9a-f]{64}");
    }

    @Test
    void signReturnsDifferentDigestsForDifferentPayloads() {
        HmacAuthSigner signer = new HmacAuthSigner(SECRET);
        assertThat(signer.sign("alpha")).isNotEqualTo(signer.sign("beta"));
    }

    @Test
    void signReturnsDifferentDigestsForDifferentSecrets() {
        String a = new HmacAuthSigner("secret-a").sign("payload");
        String b = new HmacAuthSigner("secret-b").sign("payload");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void signReturnsNullWhenDisabled() {
        assertThat(new HmacAuthSigner(null).sign("payload")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void signReturnsNullForBlankConfiguration(String secret) {
        assertThat(new HmacAuthSigner(secret).sign("payload")).isNull();
    }

    @Test
    void signReturnsNullForNullPayload() {
        HmacAuthSigner signer = new HmacAuthSigner(SECRET);
        assertThat(signer.sign(null)).isNull();
    }

    @Test
    void verifySucceedsForMatchingSignature() {
        HmacAuthSigner signer = new HmacAuthSigner(SECRET);
        String signature = signer.sign("payload");
        assertThat(signer.verify("payload", signature)).isTrue();
    }

    @Test
    void verifyFailsForTamperedPayload() {
        HmacAuthSigner signer = new HmacAuthSigner(SECRET);
        String signature = signer.sign("payload");
        assertThat(signer.verify("tampered", signature)).isFalse();
    }

    @Test
    void verifyFailsForMismatchedLength() {
        HmacAuthSigner signer = new HmacAuthSigner(SECRET);
        assertThat(signer.verify("payload", "short")).isFalse();
    }

    @Test
    void verifyFailsWhenDisabled() {
        HmacAuthSigner disabled = new HmacAuthSigner(null);
        assertThat(disabled.verify("payload", "anything")).isFalse();
    }

    @Test
    void verifyFailsForNullArguments() {
        HmacAuthSigner signer = new HmacAuthSigner(SECRET);
        assertThat(signer.verify(null, "sig")).isFalse();
        assertThat(signer.verify("payload", null)).isFalse();
    }
}
