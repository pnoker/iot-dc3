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
package io.github.pnoker.common.agentic.entity.model;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;

/**
 * Runtime event emitted while an agentic turn is executing.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
public record AgenticRunEvent(String type, String name, String title, String detail, long timestamp, String phase,
                              String status, String code) {

    public AgenticRunEvent {
        type = StringUtils.defaultIfBlank(type, "event");
        name = StringUtils.defaultString(name);
        title = StringUtils.defaultString(title);
        detail = StringUtils.defaultString(detail);
        timestamp = timestamp > 0 ? timestamp : now();
        phase = StringUtils.defaultString(phase);
        status = StringUtils.defaultString(status);
        code = StringUtils.defaultString(code);
    }

    public static AgenticRunEvent toolStart(String toolName, String domain, String title) {
        return new AgenticRunEvent("tool", toolName, title, domain, now(), "start", "running", null);
    }

    public static AgenticRunEvent toolResult(String toolName, boolean success, String code, String message) {
        String normalizedCode = StringUtils.defaultIfBlank(code, success ? "OK" : "ERROR");
        String status = success ? ("EMPTY".equals(normalizedCode) ? "empty" : "success") : "failed";
        return new AgenticRunEvent("tool", toolName, StringUtils.defaultIfBlank(message, "Tool completed"),
                normalizedCode, now(), "result", status, normalizedCode);
    }

    public static AgenticRunEvent toolError(String toolName, String message) {
        return new AgenticRunEvent("tool", toolName, StringUtils.defaultIfBlank(message, "Tool execution failed"),
                "ERROR", now(), "error", "failed", "ERROR");
    }

    private static long now() {
        return Instant.now().toEpochMilli();
    }

}
