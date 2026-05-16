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

/**
 * Structured return envelope for agentic tool calls.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
public record AgenticToolResult<T>(boolean success, String code, String message, T data) {

    public static <T> AgenticToolResult<T> ok(String message, T data) {
        return new AgenticToolResult<>(true, AgenticConstant.ToolResult.CODE_OK, message, data);
    }

    public static <T> AgenticToolResult<T> empty(String message, T data) {
        return new AgenticToolResult<>(true, AgenticConstant.ToolResult.CODE_EMPTY, message, data);
    }

    public static <T> AgenticToolResult<T> invalid(String message) {
        return new AgenticToolResult<>(false, AgenticConstant.ToolResult.CODE_INVALID_ARGUMENT, message, null);
    }

    public static <T> AgenticToolResult<T> notFound(String message) {
        return new AgenticToolResult<>(false, AgenticConstant.ToolResult.CODE_NOT_FOUND, message, null);
    }

    public static <T> AgenticToolResult<T> unavailable(String message) {
        return new AgenticToolResult<>(false, AgenticConstant.ToolResult.CODE_UNAVAILABLE, message, null);
    }

    public static <T> AgenticToolResult<T> error(String message) {
        return new AgenticToolResult<>(false, AgenticConstant.ToolResult.CODE_ERROR, message, null);
    }

}
