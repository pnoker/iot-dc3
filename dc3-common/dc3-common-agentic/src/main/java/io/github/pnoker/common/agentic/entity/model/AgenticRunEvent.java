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

import io.github.pnoker.common.constant.service.AgenticConstant;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;

/**
 * Runtime event emitted while an agentic turn is executing.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
public record AgenticRunEvent(String type, String name, String title, String detail, long timestamp, String phase,
                              String status, String code) {

    public AgenticRunEvent {
        type = StringUtils.defaultIfBlank(type, AgenticConstant.RunEvent.TYPE_EVENT);
        name = StringUtils.defaultString(name);
        title = StringUtils.defaultString(title);
        detail = StringUtils.defaultString(detail);
        timestamp = timestamp > 0 ? timestamp : now();
        phase = StringUtils.defaultString(phase);
        status = StringUtils.defaultString(status);
        code = StringUtils.defaultString(code);
    }

    public static AgenticRunEvent toolStart(String toolName, String domain, String title) {
        return new AgenticRunEvent(AgenticConstant.RunEvent.TYPE_TOOL, toolName, title, domain, now(),
                AgenticConstant.RunEvent.PHASE_START, AgenticConstant.RunEvent.STATUS_RUNNING, null);
    }

    public static AgenticRunEvent toolResult(String toolName, boolean success, String code, String message) {
        String normalizedCode = StringUtils.defaultIfBlank(code,
                success ? AgenticConstant.ToolResult.CODE_OK : AgenticConstant.ToolResult.CODE_ERROR);
        String status = success
                ? (AgenticConstant.ToolResult.CODE_EMPTY.equals(normalizedCode)
                   ? AgenticConstant.RunEvent.STATUS_EMPTY : AgenticConstant.RunEvent.STATUS_SUCCESS)
                : AgenticConstant.RunEvent.STATUS_FAILED;
        return new AgenticRunEvent(AgenticConstant.RunEvent.TYPE_TOOL, toolName,
                StringUtils.defaultIfBlank(message, AgenticConstant.ToolResult.MESSAGE_COMPLETED),
                normalizedCode, now(), AgenticConstant.RunEvent.PHASE_RESULT, status, normalizedCode);
    }

    public static AgenticRunEvent toolError(String toolName, String message) {
        return new AgenticRunEvent(AgenticConstant.RunEvent.TYPE_TOOL, toolName,
                StringUtils.defaultIfBlank(message, AgenticConstant.ToolResult.MESSAGE_EXECUTION_FAILED),
                AgenticConstant.ToolResult.CODE_ERROR, now(), AgenticConstant.RunEvent.PHASE_ERROR,
                AgenticConstant.RunEvent.STATUS_FAILED, AgenticConstant.ToolResult.CODE_ERROR);
    }

    public static AgenticRunEvent reasoningRequested() {
        return new AgenticRunEvent(AgenticConstant.RunEvent.TYPE_REASONING, AgenticConstant.RunEvent.NAME_AGENTIC,
                "Thinking", "Reasoning mode requested for this model.", now(),
                AgenticConstant.RunEvent.PHASE_START, AgenticConstant.RunEvent.STATUS_RUNNING, null);
    }

    public static AgenticRunEvent requestFailed(String message) {
        return new AgenticRunEvent(AgenticConstant.RunEvent.TYPE_ERROR, AgenticConstant.RunEvent.NAME_AGENTIC,
                AgenticConstant.ToolMessage.REQUEST_FAILED,
                StringUtils.defaultIfBlank(message, AgenticConstant.ToolMessage.REQUEST_FAILED), now(),
                AgenticConstant.RunEvent.PHASE_ERROR, AgenticConstant.RunEvent.STATUS_FAILED,
                AgenticConstant.ToolResult.CODE_ERROR);
    }

    private static long now() {
        return Instant.now().toEpochMilli();
    }

}
