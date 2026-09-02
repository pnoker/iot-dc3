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
package io.github.pnoker.common.gateway.security;

import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.OAuthJwtVerifier;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves RSA public keys from the auth center's JWKS document with a small cache.
 *
 * <p>An unknown kid triggers exactly one forced refresh before the lookup fails, so a
 * key rotation is picked up within one request instead of one cache TTL.
 *
 * @author pnoker
 */
@Slf4j
public class JwksKeySource implements OAuthJwtVerifier.KeySource {

    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final URI jwksUri;

    private final Duration refreshInterval;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private volatile Map<String, RSAPublicKey> cachedKeys = Map.of();

    private volatile long fetchedAtMillis = Long.MIN_VALUE;

    /**
     * Fetches the initial key set eagerly so a misconfigured url fails fast at startup.
     *
     * @param jwksUrl         auth center JWKS document url
     * @param refreshInterval cached document reuse window; an unknown kid always bypasses it
     */
    public JwksKeySource(String jwksUrl, Duration refreshInterval) {
        this.jwksUri = URI.create(jwksUrl);
        this.refreshInterval = refreshInterval;
        this.cachedKeys = fetch();
        this.fetchedAtMillis = System.currentTimeMillis();
    }

    @Override
    public RSAPublicKey forKeyId(String kid) {
        RSAPublicKey known = cachedKeys.get(kid);
        if (known != null) {
            return known;
        }
        // Unknown kid: force one refresh (ignoring the reuse window) so rotation is
        // immediate, then give up.
        fetchedAtMillis = Long.MIN_VALUE;
        Map<String, RSAPublicKey> refreshed = fetch();
        if (!refreshed.isEmpty()) {
            return refreshed.get(kid);
        }
        return null;
    }

    private Map<String, RSAPublicKey> fetch() {
        if (isFresh()) {
            return cachedKeys;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(jwksUri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("JWKS fetch returned {}: {}", response.statusCode(), jwksUri);
                return Map.of();
            }
            JwksDocument document = JsonUtil.parseObject(response.body(), JwksDocument.class);
            if (document == null
                    || document.getKeys() == null
                    || document.getKeys().isEmpty()) {
                log.warn("JWKS document from {} contains no keys", jwksUri);
                return Map.of();
            }
            ConcurrentHashMap<String, RSAPublicKey> result = new ConcurrentHashMap<>();
            for (JwkKey key : document.getKeys()) {
                RSAPublicKey parsed = parseKey(key);
                if (parsed != null) {
                    result.put(key.getKid(), parsed);
                }
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of();
        } catch (Exception e) {
            log.warn("JWKS fetch failed for {}: {}", jwksUri, e.getMessage());
            return Map.of();
        }
    }

    private boolean isFresh() {
        return fetchedAtMillis != Long.MIN_VALUE
                && System.currentTimeMillis() - fetchedAtMillis < refreshInterval.toMillis();
    }

    private RSAPublicKey parseKey(JwkKey key) {
        if (key == null
                || !"RSA".equals(key.getKty())
                || key.getKid() == null
                || key.getN() == null
                || key.getE() == null) {
            return null;
        }
        try {
            BigInteger modulus = new BigInteger(1, URL_DECODER.decode(key.getN()));
            BigInteger exponent = new BigInteger(1, URL_DECODER.decode(key.getE()));
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (Exception e) {
            log.warn("Skipping unusable JWKS key {}: {}", key.getKid(), e.getMessage());
            return null;
        }
    }

    /**
     * Minimal JWKS document shape.
     */
    @lombok.Getter
    @lombok.Setter
    public static class JwksDocument {
        private List<JwkKey> keys;
    }

    /**
     * Single JWK entry.
     */
    @lombok.Getter
    @lombok.Setter
    public static class JwkKey {
        private String kty;
        private String kid;
        private String use;
        private String alg;
        private String n;
        private String e;
    }
}
