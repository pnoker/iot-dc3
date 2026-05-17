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

import io.github.pnoker.common.constant.common.EnvironmentConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * HMAC-SHA256 signer for the {@code X-Auth-User} header.
 * <p>
 * The gateway computes a signature of the user JSON and forwards it as {@code X-Auth-Sign}.
 * Backend services verify the signature before trusting any tenant/user identity claims —
 * without this, any client able to reach a backend port directly can spoof any tenant by
 * crafting their own {@code X-Auth-User} header.
 * <p>
 * The shared secret is read from {@code dc3.auth.hmac.secret} (or the {@code
 * AUTH_HMAC_SECRET} environment variable). When neither is set, signing is disabled and a
 * loud warning is emitted at startup — backends fall back to the unverified behaviour, so
 * existing deployments don't break, but production should always set a strong secret.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class HmacAuthSigner {

    private static final String ALGO = "HmacSHA256";

    private final byte[] secret;

    private final boolean enabled;

    public HmacAuthSigner(String secret) {
        if (StringUtils.isBlank(secret)) {
            this.secret = null;
            this.enabled = false;
            log.warn(
                    "{} (env {}) is not configured. X-Auth-User header signing is DISABLED. "
                            + "Backend services will trust the X-Auth-User header without verification, which is unsafe in production. "
                            + "Set a strong shared secret to enable signing.",
                    EnvironmentConstant.AUTH_HMAC_SECRET_PROPERTY, EnvironmentConstant.AUTH_HMAC_SECRET_ENV);
        } else {
            this.secret = secret.getBytes(StandardCharsets.UTF_8);
            this.enabled = true;
            log.info("X-Auth-User header signing enabled (HMAC-SHA256).");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return hex-encoded HMAC of {@code payload}, or {@code null} when signing is disabled
     * or the payload is null.
     */
    public String sign(String payload) {
        if (!enabled || Objects.isNull(payload)) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret, ALGO));
            byte[] sig = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(sig);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }

    /**
     * Constant-time verification of a signature against a payload. Caller must check
     * {@link #isEnabled()} first if it wants to skip verification when signing is disabled.
     *
     * @return {@code true} iff the supplied hex signature matches the computed HMAC.
     */
    public boolean verify(String payload, String expectedHex) {
        if (!enabled || Objects.isNull(payload) || Objects.isNull(expectedHex)) {
            return false;
        }
        String actual = sign(payload);
        if (Objects.isNull(actual) || actual.length() != expectedHex.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < actual.length(); i++) {
            diff |= actual.charAt(i) ^ expectedHex.charAt(i);
        }
        return diff == 0;
    }

}
