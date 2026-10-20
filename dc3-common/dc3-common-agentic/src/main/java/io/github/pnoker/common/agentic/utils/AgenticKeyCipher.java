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

import io.github.pnoker.common.agentic.config.AgenticProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * AES-256-GCM envelope encryption for provider API keys at the persistence
 * boundary. Ciphertext format: {@code enc:v1:<base64(iv || ciphertext || tag)>}
 * with a random 12-byte IV and a 128-bit tag per encryption.
 *
 * <p>Master key resolution order:
 * <ol>
 *   <li>{@code dc3.agentic.security-master-key} (Base64, 256-bit)</li>
 *   <li>{@code DC3_AGENTIC_MASTER_KEY} environment variable (Base64, 256-bit)</li>
 *   <li>HKDF-SHA256 derivation from the platform {@code DC3_SECURITY_KEY}
 *       (environment variable, falling back to the {@code dc3.security.key}
 *       system property) — stable across restarts, warned once at startup;
 *       production deployments should set an explicit key instead</li>
 * </ol>
 *
 * <p>Backward compatibility: values without the {@code enc:v1:} prefix are
 * legacy plaintext and are returned as-is on read, then transparently
 * upgraded to ciphertext on the next write (lazy migration). Blank keys pass
 * through unchanged both ways — an empty API key is a valid no-key provider.
 * Decryption failures fail loudly with a clear cause; they are never
 * swallowed into a silent plaintext fallback.
 *
 * @author pnoker
 * @since 2026.10.10
 */
@Slf4j
public final class AgenticKeyCipher {

    /** Ciphertext prefix marking an encrypted value. */
    public static final String CIPHERTEXT_PREFIX = "enc:v1:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final int KEY_BYTES = 32;
    private static final byte[] HKDF_SALT = "dc3-agentic-key-cipher-hkdf-salt".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HKDF_INFO =
            "dc3-agentic-provider-api-key-aes-256-gcm-v1".getBytes(StandardCharsets.UTF_8);
    private static final String MASTER_KEY_ENV = "DC3_AGENTIC_MASTER_KEY";
    private static final String PLATFORM_KEY_ENV = "DC3_SECURITY_KEY";
    private static final String PLATFORM_KEY_PROPERTY = "dc3.security.key";

    private static final AtomicBoolean WARNED_DERIVED_KEY = new AtomicBoolean();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey masterKey;

    /**
     * Create a cipher resolving the master key per the documented order.
     *
     * @param properties agentic properties carrying the explicit master key;
     *                   may be null when the agentic module is disabled and
     *                   only the store layer is on the classpath
     */
    public AgenticKeyCipher(AgenticProperties properties) {
        this(properties, AgenticKeyCipher::platformSecurityKey);
    }

    /** Test constructor with an injectable platform-security-key source. */
    AgenticKeyCipher(AgenticProperties properties, java.util.function.Supplier<String> platformSecurityKey) {
        this.masterKey = resolveMasterKey(properties, platformSecurityKey);
    }

    /**
     * Whether the stored value is already encrypted.
     *
     * @param value stored value, may be null
     * @return true when the value carries the ciphertext prefix
     */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(CIPHERTEXT_PREFIX);
    }

    /**
     * Encrypt a plaintext API key for storage. Blank input passes through
     * unchanged (nothing to protect).
     *
     * @param plain plaintext API key, may be null or blank
     * @return ciphertext string, or the input when blank
     * @throws IllegalStateException when no master key material is configured
     */
    public String encrypt(String plain) {
        if (StringUtils.isBlank(plain)) {
            return plain;
        }
        requireMasterKey();
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] envelope = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, envelope, 0, iv.length);
            System.arraycopy(encrypted, 0, envelope, iv.length, encrypted.length);
            return CIPHERTEXT_PREFIX + Base64.getEncoder().encodeToString(envelope);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Agentic provider API key encryption failed", exception);
        }
    }

    /**
     * Decrypt a stored API key. Values without the ciphertext prefix are
     * legacy plaintext and are returned unchanged (lazy migration re-encrypts
     * on the next write); blank input passes through.
     *
     * @param stored stored value, may be null or blank
     * @return plaintext API key
     * @throws IllegalStateException when the value is encrypted but cannot be
     *                               decrypted — wrong or missing master key,
     *                               corrupted ciphertext
     */
    public String decrypt(String stored) {
        if (StringUtils.isBlank(stored)) {
            return stored;
        }
        if (!isEncrypted(stored)) {
            return stored;
        }
        requireMasterKey();
        byte[] envelope;
        try {
            envelope = Base64.getDecoder().decode(stored.substring(CIPHERTEXT_PREFIX.length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Agentic provider API key ciphertext is malformed (invalid Base64)", exception);
        }
        if (envelope.length <= IV_BYTES) {
            throw new IllegalStateException("Agentic provider API key ciphertext is truncated");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, envelope, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(envelope, IV_BYTES, envelope.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Agentic provider API key could not be decrypted — the configured master key "
                            + "may have changed or the stored value is corrupted",
                    exception);
        }
    }

    private void requireMasterKey() {
        if (masterKey == null) {
            throw new IllegalStateException(String.format(
                    Locale.ROOT,
                    "Agentic provider API key encryption requires key material: set %s, "
                            + "dc3.agentic.security-master-key, or %s",
                    MASTER_KEY_ENV,
                    PLATFORM_KEY_ENV));
        }
    }

    private static SecretKey resolveMasterKey(
            AgenticProperties properties, java.util.function.Supplier<String> platformSecurityKey) {
        byte[] explicit = decodeExplicitMasterKey(properties);
        if (explicit == null) {
            explicit = decodeBase64Key(System.getenv(MASTER_KEY_ENV), MASTER_KEY_ENV);
        }
        if (explicit != null) {
            return new SecretKeySpec(explicit, "AES");
        }
        String platformKey = platformSecurityKey.get();
        if (platformKey != null && !platformKey.isBlank()) {
            if (WARNED_DERIVED_KEY.compareAndSet(false, true)) {
                log.warn(
                        "Agentic provider key encryption derives its master key from {}; set {} "
                                + "(Base64, 256-bit) or dc3.agentic.security-master-key in production",
                        PLATFORM_KEY_ENV,
                        MASTER_KEY_ENV);
            }
            return new SecretKeySpec(hkdfSha256(platformKey), "AES");
        }
        log.warn(
                "No master key material for agentic provider key encryption; set {} or {} before "
                        + "storing provider API keys",
                MASTER_KEY_ENV,
                PLATFORM_KEY_ENV);
        return null;
    }

    private static byte[] decodeExplicitMasterKey(AgenticProperties properties) {
        if (properties == null) {
            return null;
        }
        return decodeBase64Key(properties.getSecurityMasterKey(), "dc3.agentic.security-master-key");
    }

    private static byte[] decodeBase64Key(String value, String source) {
        if (value == null || value.isBlank()) {
            return null;
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    String.format(Locale.ROOT, "Agentic master key from %s is not valid Base64", source), exception);
        }
        if (key.length != KEY_BYTES) {
            throw new IllegalStateException(String.format(
                    Locale.ROOT,
                    "Agentic master key from %s must be %d bytes (Base64 of a 256-bit key), got %d bytes",
                    source,
                    KEY_BYTES,
                    key.length));
        }
        return key;
    }

    private static String platformSecurityKey() {
        String key = System.getenv(PLATFORM_KEY_ENV);
        if (key == null || key.isBlank()) {
            key = System.getProperty(PLATFORM_KEY_PROPERTY);
        }
        return key;
    }

    /** RFC 5869 HKDF with HMAC-SHA256: extract with a fixed salt, expand to 32 bytes. */
    private static byte[] hkdfSha256(String inputKeyMaterial) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HKDF_SALT, "HmacSHA256"));
            byte[] prk = mac.doFinal(inputKeyMaterial.getBytes(StandardCharsets.UTF_8));
            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            byte[] block = new byte[0];
            byte[] output = new byte[KEY_BYTES];
            int produced = 0;
            int counter = 1;
            while (produced < KEY_BYTES) {
                mac.update(block);
                mac.update(HKDF_INFO);
                mac.update((byte) counter++);
                block = mac.doFinal();
                int take = Math.min(block.length, KEY_BYTES - produced);
                System.arraycopy(block, 0, output, produced, take);
                produced += take;
            }
            return output;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Agentic master key derivation failed", exception);
        }
    }
}
