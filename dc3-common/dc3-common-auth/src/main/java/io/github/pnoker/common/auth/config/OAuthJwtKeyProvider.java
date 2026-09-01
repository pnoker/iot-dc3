package io.github.pnoker.common.auth.config;

import io.github.pnoker.common.utils.OAuthJwtVerifier;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Strict verification-key source for OAuth access tokens.
 *
 * <p>The runtime accepts exactly one configured key id. Missing or malformed key
 * material is a configuration error; generating an in-process key would invalidate
 * every token after a restart and is therefore never a valid production fallback.</p>
 */
@Component
@RequiredArgsConstructor
public final class OAuthJwtKeyProvider implements OAuthJwtVerifier.KeySource {
    public static final String KEY_ID = "dc3-oauth-rsa";

    private final OAuthProperties properties;
    private volatile RSAPublicKey cached;
    private volatile PrivateKey signing;

    @Override
    public RSAPublicKey forKeyId(String kid) {
        if (!KEY_ID.equals(kid)) {
            return null;
        }
        return verificationKey();
    }

    public RSAPublicKey verificationKey() {
        RSAPublicKey current = cached;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cached == null) {
                cached = load();
            }
            return cached;
        }
    }

    /**
     * Resolve the configured private key used to sign OAuth access tokens.
     * Ephemeral signing keys are intentionally forbidden: a restart must not
     * invalidate the verification contract or produce unverifiable tokens.
     */
    public PrivateKey signingKey() {
        PrivateKey current = signing;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (signing == null) {
                String encoded = properties.getJwt() == null ? null : properties.getJwt().getPrivateKey();
                if (encoded == null || encoded.isBlank()) {
                    throw new JwtException("OAuth JWT private key is not configured");
                }
                try {
                    KeyFactory factory = KeyFactory.getInstance("RSA");
                    signing = factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
                } catch (Exception error) {
                    throw new JwtException("OAuth JWT private key is invalid", error);
                }
            }
            return signing;
        }
    }

    private RSAPublicKey load() {
        String encoded = properties.getJwt() == null ? null : properties.getJwt().getPublicKey();
        if (encoded == null || encoded.isBlank()) {
            throw new JwtException("OAuth JWT public key is not configured");
        }
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
        } catch (Exception error) {
            throw new JwtException("OAuth JWT public key is invalid", error);
        }
    }
}
