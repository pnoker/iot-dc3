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
package io.github.pnoker.common.agentic.service.check;

import io.github.pnoker.common.enums.ConnectivityErrorTypeEnum;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLException;
import org.apache.commons.lang3.StringUtils;

/**
 * Maps probe failures to a {@link ConnectivityErrorTypeEnum} so the frontend
 * can render a targeted fix hint. Pure function: exception type first, then
 * HTTP status, then error-message keywords (English and Chinese relay-gateway
 * messages included).
 *
 * @author pnoker
 * @since 2016.10.1
 */
public final class ConnectivityErrorClassifier {

    private static final int MESSAGE_MAX_LENGTH = 255;

    private static final List<String> QUOTA_KEYWORDS = List.of(
            "insufficient quota",
            "insufficient balance",
            "quota exceeded",
            "exceeded your current quota",
            "billing",
            "欠费",
            "余额不足",
            "无可用渠道",
            "no available channel");

    private static final List<String> MODEL_KEYWORDS =
            List.of("model_not_found", "model not found", "does not exist", "unknown model", "模型不存在");

    private static final List<String> AUTH_KEYWORDS =
            List.of("invalid_api_key", "invalid api key", "api key not valid", "incorrect api key", "authentication");

    private ConnectivityErrorClassifier() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Classify a probe failure.
     *
     * @param upstreamStatus upstream HTTP status when a response arrived, else null
     * @param error          transport exception when no response arrived, else null
     * @param bodySnippet    upstream error body excerpt, may be null
     * @return classified error type, never null
     */
    public static ConnectivityErrorTypeEnum classify(Integer upstreamStatus, Throwable error, String bodySnippet) {
        if (error != null && !isTimeout(error)) {
            ConnectivityErrorTypeEnum byTransport = classifyByTransport(error);
            if (byTransport != null) return byTransport;
        }
        if (isTimeout(error) || containsKeyword(bodySnippet, List.of("timeout", "timed out"))) {
            return ConnectivityErrorTypeEnum.TIMEOUT;
        }
        if (upstreamStatus != null) {
            if (upstreamStatus >= 500) {
                return ConnectivityErrorTypeEnum.UPSTREAM_ERROR;
            }
            return switch (upstreamStatus) {
                case 401, 403 -> ConnectivityErrorTypeEnum.AUTH_FAILED;
                case 404 -> ConnectivityErrorTypeEnum.MODEL_NOT_FOUND;
                case 429 -> ConnectivityErrorTypeEnum.RATE_LIMITED;
                default -> unknownStatus(bodySnippet);
            };
        }
        ConnectivityErrorTypeEnum byBody = classifyByBody(bodySnippet, null);
        return byBody != null ? byBody : ConnectivityErrorTypeEnum.UPSTREAM_ERROR;
    }

    private static boolean isTimeout(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SocketTimeoutException || current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static ConnectivityErrorTypeEnum classifyByTransport(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof UnknownHostException || current instanceof ConnectException) {
                return ConnectivityErrorTypeEnum.UNREACHABLE;
            }
            if (current instanceof SSLException) {
                return ConnectivityErrorTypeEnum.TLS_ERROR;
            }
            if (current instanceof IOException
                    && current.getMessage() != null
                    && current.getMessage().toLowerCase(Locale.ROOT).contains("timeout")) {
                return ConnectivityErrorTypeEnum.TIMEOUT;
            }
        }
        return null;
    }

    private static ConnectivityErrorTypeEnum unknownStatus(String bodySnippet) {
        ConnectivityErrorTypeEnum byBody = classifyByBody(bodySnippet, null);
        return byBody != null ? byBody : ConnectivityErrorTypeEnum.BAD_RESPONSE;
    }

    private static ConnectivityErrorTypeEnum classifyByBody(String bodySnippet, Integer upstreamStatus) {
        if (StringUtils.isBlank(bodySnippet)) {
            return upstreamStatus == null ? null : ConnectivityErrorTypeEnum.UPSTREAM_ERROR;
        }
        if (containsKeyword(bodySnippet, QUOTA_KEYWORDS)) return ConnectivityErrorTypeEnum.QUOTA_EXCEEDED;
        if (containsKeyword(bodySnippet, MODEL_KEYWORDS)) return ConnectivityErrorTypeEnum.MODEL_NOT_FOUND;
        if (containsKeyword(bodySnippet, AUTH_KEYWORDS)) return ConnectivityErrorTypeEnum.AUTH_FAILED;
        return upstreamStatus == null ? null : ConnectivityErrorTypeEnum.UPSTREAM_ERROR;
    }

    private static boolean containsKeyword(String bodySnippet, List<String> keywords) {
        if (StringUtils.isBlank(bodySnippet)) return false;
        String lowered = bodySnippet.toLowerCase(Locale.ROOT);
        return keywords.stream().anyMatch(lowered::contains);
    }

    /**
     * Sanitize an upstream error message for storage and display: collapse line
     * breaks, mask the probe credential when the upstream echoes it back, and
     * truncate to the {@code last_check_error_message} column width.
     *
     * @param message raw upstream error text, may be null
     * @param apiKey  the credential used by the probe, masked when present
     * @return sanitized one-line message, empty string when the input is blank
     */
    public static String sanitizeMessage(String message, String apiKey) {
        String value = StringUtils.defaultString(message).replace("\r", "\\r").replace("\n", "\\t");
        if (StringUtils.isNotBlank(apiKey)) {
            value = value.replace(apiKey, "***");
        }
        return StringUtils.truncate(value.trim(), MESSAGE_MAX_LENGTH);
    }
}
