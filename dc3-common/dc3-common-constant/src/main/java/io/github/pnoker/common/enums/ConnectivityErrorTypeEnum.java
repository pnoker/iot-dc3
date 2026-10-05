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
package io.github.pnoker.common.enums;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Connectivity check error type. Classifies why an L1 (model-list) or L2
 * (minimal chat) probe failed so the frontend can render a targeted fix hint.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum ConnectivityErrorTypeEnum {

    /**
     * The provider has no API key configured. Legal for local endpoints
     * (Ollama/vLLM/LM Studio); surfaced as information, never as a hard failure.
     */
    NO_KEY_CONFIGURED((byte) 0, "no-key-configured", "No API key configured"),

    /**
     * DNS resolution or TCP connection to the base URL failed.
     */
    UNREACHABLE((byte) 1, "unreachable", "Base URL unreachable"),

    /**
     * TLS handshake or certificate validation failed, e.g. self-signed certs.
     */
    TLS_ERROR((byte) 2, "tls-error", "TLS handshake failed"),

    /**
     * Upstream rejected the credentials (HTTP 401/403).
     */
    AUTH_FAILED((byte) 3, "auth-failed", "Authentication failed"),

    /**
     * The requested model does not exist or is not visible to the key (HTTP 404
     * or the model list does not contain it).
     */
    MODEL_NOT_FOUND((byte) 4, "model-not-found", "Model not found"),

    /**
     * Upstream rate limiting (HTTP 429).
     */
    RATE_LIMITED((byte) 5, "rate-limited", "Rate limited"),

    /**
     * Quota or balance exhausted, detected by status or error keywords
     * (including Chinese relay-gateway messages).
     */
    QUOTA_EXCEEDED((byte) 6, "quota-exceeded", "Quota or balance exhausted"),

    /**
     * The probe exceeded its timeout budget.
     */
    TIMEOUT((byte) 7, "timeout", "Probe timed out"),

    /**
     * Upstream server error (HTTP 5xx).
     */
    UPSTREAM_ERROR((byte) 8, "upstream-error", "Upstream server error"),

    /**
     * The endpoint answered but not in a standard OpenAI/Anthropic shape,
     * so the response could not be parsed.
     */
    BAD_RESPONSE((byte) 9, "bad-response", "Malformed upstream response"),
    ;

    /**
     * Index value stored in the request/response contract.
     */
    private final Byte index;

    /**
     * Code string.
     */
    private final String code;

    /**
     * Human-readable description.
     */
    private final String remark;

    /**
     * Get enum by index value.
     *
     * @param index index value
     * @return {@link ConnectivityErrorTypeEnum} or {@code null} if not found
     */
    public static ConnectivityErrorTypeEnum ofIndex(Byte index) {
        Optional<ConnectivityErrorTypeEnum> any = Arrays.stream(ConnectivityErrorTypeEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code string.
     *
     * @param code code string
     * @return {@link ConnectivityErrorTypeEnum} or {@code null} if not found
     */
    public static ConnectivityErrorTypeEnum ofCode(String code) {
        Optional<ConnectivityErrorTypeEnum> any = Arrays.stream(ConnectivityErrorTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by enum name.
     *
     * @param name enum name
     * @return {@link ConnectivityErrorTypeEnum} or {@code null} if parsing fails
     */
    public static ConnectivityErrorTypeEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
