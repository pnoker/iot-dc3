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

import io.github.pnoker.common.constant.service.AgenticConstant;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Redacts credentials from upstream provider error messages before they reach
 * the tenant-facing run trace or persistence. Provider 401 bodies echo API keys
 * ("Incorrect API key provided: sk-…"), and with transport fallback enabled the
 * echoed key may be the platform key, so raw upstream text must never cross the
 * tenant boundary; the full message stays in the server-side log.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
public final class AgenticUpstreamErrorSanitizer {

    /** Bound on the redacted message replayed to the client. */
    private static final int MAX_CHARS = 300;

    private static final Pattern API_KEY_LITERAL = Pattern.compile("sk-[A-Za-z0-9_-]{6,}");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/=-]{6,}");
    private static final Pattern CREDENTIAL_ASSIGNMENT =
            Pattern.compile("(?i)(api[_-]?key|authorization|token|secret|password)(\\s*[:=]\\s*)\"?[^\\s\",&]+\"?");

    private AgenticUpstreamErrorSanitizer() {}

    /**
     * Map an upstream failure to a bounded, credential-free message for the run
     * trace. Blank messages collapse to the generic request-failed text; the
     * full exception remains available to the server log via the caller's log.
     *
     * @param error upstream error
     * @return redacted message safe for tenant-facing replay
     */
    public static String sanitize(Throwable error) {
        if (error == null) {
            return AgenticConstant.ToolMessage.REQUEST_FAILED;
        }
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return AgenticConstant.ToolMessage.REQUEST_FAILED;
        }
        return sanitize(message);
    }

    /**
     * Redact credential-shaped substrings and cap the length of an upstream
     * error message.
     *
     * @param message raw upstream message
     * @return redacted message safe for tenant-facing replay
     */
    public static String sanitize(String message) {
        String redacted = API_KEY_LITERAL.matcher(message).replaceAll("sk-***");
        redacted = BEARER_TOKEN.matcher(redacted).replaceAll("Bearer ***");
        redacted = CREDENTIAL_ASSIGNMENT.matcher(redacted).replaceAll("$1$2***");
        if (redacted.length() > MAX_CHARS) {
            redacted = redacted.substring(0, MAX_CHARS) + "…";
        }
        return redacted;
    }
}
