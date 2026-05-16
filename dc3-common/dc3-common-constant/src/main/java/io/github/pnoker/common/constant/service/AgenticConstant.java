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

package io.github.pnoker.common.constant.service;

import io.github.pnoker.common.constant.common.BaseConstant;


/**
 * Agentic service related constants.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public class AgenticConstant {

    /**
     * Service name registered in service discovery.
     */
    public static final String SERVICE_NAME = "dc3-center-agentic";

    /**
     * URL prefix for the chat REST API. Gateway: /api/v3/agentic/chat/** ->
     * agentic:8600/agentic/chat/**
     */
    public static final String CHAT_URL_PREFIX = "/chat";

    /**
     * URL prefix for the session REST API.
     */
    public static final String SESSION_URL_PREFIX = "/session";

    /**
     * URL prefix for the model REST API.
     */
    public static final String MODEL_URL_PREFIX = "/model";

    /**
     * URL prefix for the provider REST API.
     */
    public static final String PROVIDER_URL_PREFIX = "/provider";

    /**
     * URL prefix for the message REST API.
     */
    public static final String MESSAGE_URL_PREFIX = "/message";

    /**
     * URL prefix for the attachment REST API.
     */
    public static final String ATTACHMENT_URL_PREFIX = "/attachment";

    /**
     * URL prefix for the action confirmation REST API.
     */
    public static final String ACTION_URL_PREFIX = "/action";

    private AgenticConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    /**
     * Spring AI tool context keys used to pass request scope into tool calls.
     *
     * @author pnoker
     * @version 2025.9.0
     * @since 2022.1.0
     */
    public static class ToolContextKey {

        public static final String TENANT_ID = "dc3.agentic.tenantId";

        public static final String USER_ID = "dc3.agentic.userId";

        public static final String USER_HEADER = "dc3.agentic.userHeader";

        public static final String CONVERSATION_ID = "dc3.agentic.conversationId";

        public static final String RUN_EVENTS = "dc3.agentic.runEvents";

        private ToolContextKey() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * OpenAI-compatible chat response constants.
     *
     * @author pnoker
     * @version 2026.5.16
     * @since 2022.1.0
     */
    public static class Chat {

        public static final String COMPLETION_OBJECT = "chat.completion";

        public static final String COMPLETION_CHUNK_OBJECT = "chat.completion.chunk";

        public static final String ID_PREFIX = "chatcmpl-";

        public static final String STREAM_DONE = "[DONE]";

        public static final String ROLE_SYSTEM = "system";

        public static final String ROLE_USER = "user";

        public static final String ROLE_ASSISTANT = "assistant";

        public static final String ROLE_TOOL = "tool";

        public static final String FINISH_REASON_STOP = "stop";

        public static final String FINISH_REASON_ERROR = "error";

        private Chat() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Agentic session constants.
     *
     * @author pnoker
     * @version 2026.5.16
     * @since 2022.1.0
     */
    public static class Session {

        public static final String DEFAULT_TITLE = "New Conversation";

        private Session() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Agentic runtime event constants.
     *
     * @author pnoker
     * @version 2026.5.16
     * @since 2022.1.0
     */
    public static class RunEvent {

        public static final String OBJECT = "agentic.event";

        public static final String TYPE_EVENT = "event";

        public static final String TYPE_TOOL = "tool";

        public static final String TYPE_REASONING = "reasoning";

        public static final String TYPE_ERROR = "error";

        public static final String NAME_AGENTIC = "agentic";

        public static final String PHASE_START = "start";

        public static final String PHASE_RESULT = "result";

        public static final String PHASE_ERROR = "error";

        public static final String STATUS_RUNNING = "running";

        public static final String STATUS_SUCCESS = "success";

        public static final String STATUS_EMPTY = "empty";

        public static final String STATUS_FAILED = "failed";

        private RunEvent() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Agentic tool result envelope constants.
     *
     * @author pnoker
     * @version 2026.5.16
     * @since 2022.1.0
     */
    public static class ToolResult {

        public static final String CODE_OK = "OK";

        public static final String CODE_EMPTY = "EMPTY";

        public static final String CODE_INVALID_ARGUMENT = "INVALID_ARGUMENT";

        public static final String CODE_NOT_FOUND = "NOT_FOUND";

        public static final String CODE_UNAVAILABLE = "UNAVAILABLE";

        public static final String CODE_ERROR = "ERROR";

        public static final String MESSAGE_COMPLETED = "Tool completed";

        public static final String MESSAGE_EXECUTION_FAILED = "Tool execution failed";

        private ToolResult() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Shared tool execution limits.
     *
     * @author pnoker
     * @version 2026.5.16
     * @since 2022.1.0
     */
    public static class ToolLimit {

        public static final int MAX_IDS = 50;

        public static final int MAX_HISTORY_RECORDS = 200;

        private ToolLimit() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Shared agentic tool messages.
     *
     * @author pnoker
     * @version 2026.5.16
     * @since 2022.1.0
     */
    public static class ToolMessage {

        public static final String STATUS_HEALTH_UNAVAILABLE =
                "Status and health tools are not available in this deployment mode.";

        public static final String PROFILE_UNAVAILABLE = "Profile tools are not available in this deployment mode.";

        public static final String SYSTEM_HEALTH_UNAVAILABLE = "System health snapshot is unavailable.";

        public static final String REQUEST_FAILED = "Request failed";

        private ToolMessage() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

}
