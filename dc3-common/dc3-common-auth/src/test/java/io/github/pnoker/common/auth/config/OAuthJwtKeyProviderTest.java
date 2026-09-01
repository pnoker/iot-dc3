package io.github.pnoker.common.auth.config;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthJwtKeyProviderTest {

    @Test
    void rejectsUnknownKeyIdBeforeReadingKeyMaterial() {
        OAuthProperties properties = new OAuthProperties();
        OAuthJwtKeyProvider provider = new OAuthJwtKeyProvider(properties);

        org.assertj.core.api.Assertions.assertThat(provider.forKeyId("untrusted-kid")).isNull();
    }

    @Test
    void rejectsMissingConfiguredPublicKey() {
        OAuthJwtKeyProvider provider = new OAuthJwtKeyProvider(new OAuthProperties());

        assertThatThrownBy(provider::verificationKey)
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("public key is not configured");
    }
}
