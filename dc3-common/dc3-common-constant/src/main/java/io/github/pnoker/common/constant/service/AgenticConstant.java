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
 * @since 2016.10.1
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
     * @since 2016.10.1
     */
    public static class ToolContextKey {

        /**
         * Tool-context key carrying the authenticated tenant id.
         */
        public static final String TENANT_ID = "dc3.agentic.tenantId";

        /**
         * Tool-context key carrying the authenticated user id.
         */
        public static final String USER_ID = "dc3.agentic.userId";

        /**
         * Tool-context key carrying the authenticated principal header.
         */
        public static final String USER_HEADER = "dc3.agentic.userHeader";

        /**
         * Tool-context key carrying the conversation id.
         */
        public static final String CONVERSATION_ID = "dc3.agentic.conversationId";

        /**
         * Tool-context key accumulating runtime events of the current run.
         */
        public static final String RUN_EVENTS = "dc3.agentic.runEvents";

        /**
         * Tool-context key accumulating structured visualizations of the current run.
         */
        public static final String VISUALIZATIONS = "dc3.agentic.visualizations";

        private ToolContextKey() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }
    }

    /**
     * OpenAI-compatible chat response constants.
     *
     * @author pnoker
     * @since 2016.10.1
     */
    public static class Chat {

        /**
         * SSE object for a full completion response.
         */
        public static final String COMPLETION_OBJECT = "chat.completion";

        /**
         * SSE object for a completion stream chunk.
         */
        public static final String COMPLETION_CHUNK_OBJECT = "chat.completion.chunk";

        /**
         * Prefix used to synthesize OpenAI-compatible response ids.
         */
        public static final String ID_PREFIX = "chatcmpl-";

        /**
         * Sentinel terminating an OpenAI-compatible completion stream.
         */
        public static final String STREAM_DONE = "[DONE]";

        /**
         * Chat role of the system message.
         */
        public static final String ROLE_SYSTEM = "system";

        /**
         * Chat role of user messages.
         */
        public static final String ROLE_USER = "user";

        /**
         * Chat role of assistant messages.
         */
        public static final String ROLE_ASSISTANT = "assistant";

        /**
         * Chat role of tool results.
         */
        public static final String ROLE_TOOL = "tool";

        /**
         * Finish reason for a normally completed response.
         */
        public static final String FINISH_REASON_STOP = "stop";

        /**
         * Finish reason for an errored response.
         */
        public static final String FINISH_REASON_ERROR = "error";

        /**
         * Finish reason for a response cancelled before normal completion.
         */
        public static final String FINISH_REASON_CANCELLED = "cancelled";

        private Chat() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }
    }

    /**
     * Structured agentic visualization constants.
     *
     * @author pnoker
     * @since 2016.10.1
     */
    public static class Visualization {

        /**
         * SSE object identifying structured visualizations.
         */
        public static final String OBJECT = "agentic.visualization";

        private Visualization() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

        /**
         * Chart type codes the visualization channel accepts (line/area/column/bar/pie/donut/heatmap/scatter/stat).
         */
        public static class Type {

            /**
             * Line chart type code.
             */
            public static final String LINE = "line";

            /**
             * Area chart type code.
             */
            public static final String AREA = "area";

            /**
             * Column chart type code.
             */
            public static final String COLUMN = "column";

            /**
             * Bar chart type code.
             */
            public static final String BAR = "bar";

            /**
             * Pie chart type code.
             */
            public static final String PIE = "pie";

            /**
             * Donut chart type code.
             */
            public static final String DONUT = "donut";

            /**
             * Heatmap chart type code.
             */
            public static final String HEATMAP = "heatmap";

            /**
             * Scatter chart type code.
             */
            public static final String SCATTER = "scatter";

            /**
             * Stat card type code.
             */
            public static final String STAT = "stat";

            private Type() {
                throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
            }
        }

        /**
         * Axis scale kinds: linear or time.
         */
        public static class Scale {

            /**
             * Linear axis scale kind.
             */
            public static final String LINEAR = "linear";

            /**
             * Time axis scale kind.
             */
            public static final String TIME = "time";

            private Scale() {
                throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
            }
        }
    }

    /**
     * Agentic session constants.
     *
     * @author pnoker
     * @since 2016.10.1
     */
    public static class Session {

        /**
         * Title assigned to sessions without a user-provided title.
         */
        public static final String DEFAULT_TITLE = "New Conversation";

        private Session() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }
    }

    /**
     * Agentic runtime event constants.
     *
     * @author pnoker
     * @since 2016.10.1
     */
    public static class RunEvent {

        /**
         * SSE object identifying structured visualizations.
         */
        public static final String OBJECT = "agentic.event";

        /**
         * Run-event type for plain lifecycle events.
         */
        public static final String TYPE_EVENT = "event";

        /**
         * Run-event type for tool invocations.
         */
        public static final String TYPE_TOOL = "tool";

        /**
         * Run-event type for reasoning steps.
         */
        public static final String TYPE_REASONING = "reasoning";

        /**
         * Run-event type for failures.
         */
        public static final String TYPE_ERROR = "error";

        /**
         * Emitter name shared by all agentic run events.
         */
        public static final String NAME_AGENTIC = "agentic";

        /**
         * Run-event phase marking the start of a step.
         */
        public static final String PHASE_START = "start";

        /**
         * Run-event phase marking the result of a step.
         */
        public static final String PHASE_RESULT = "result";

        /**
         * Run-event phase marking a failed step.
         */
        public static final String PHASE_ERROR = "error";

        /**
         * Run-event status for in-flight steps.
         */
        public static final String STATUS_RUNNING = "running";

        /**
         * Run-event status for completed steps.
         */
        public static final String STATUS_SUCCESS = "success";

        /**
         * Run-event status for steps with no data.
         */
        public static final String STATUS_EMPTY = "empty";

        /**
         * Run-event status for failed steps.
         */
        public static final String STATUS_FAILED = "failed";

        private RunEvent() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }
    }

    /**
     * Agentic tool result envelope constants.
     *
     * @author pnoker
     * @since 2016.10.1
     */
    public static class ToolResult {

        /**
         * Tool-result field carrying structured visualizations.
         */
        public static final String FIELD_VISUALIZATIONS = "visualizations";

        /**
         * Tool result code for success.
         */
        public static final String CODE_OK = "OK";

        /**
         * Tool result code for empty results.
         */
        public static final String CODE_EMPTY = "EMPTY";

        /**
         * Tool result code for invalid arguments.
         */
        public static final String CODE_INVALID_ARGUMENT = "INVALID_ARGUMENT";

        /**
         * Tool result code for missing resources.
         */
        public static final String CODE_NOT_FOUND = "NOT_FOUND";

        /**
         * Tool result code for unavailable capabilities.
         */
        public static final String CODE_UNAVAILABLE = "UNAVAILABLE";

        /**
         * Tool result code for failures.
         */
        public static final String CODE_ERROR = "ERROR";

        /**
         * Default success message for tool results.
         */
        public static final String MESSAGE_COMPLETED = "Tool completed";

        /**
         * Default failure message for tool results.
         */
        public static final String MESSAGE_EXECUTION_FAILED = "Tool execution failed";

        private ToolResult() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }
    }

    /**
     * Shared tool execution limits.
     *
     * @author pnoker
     * @since 2016.10.1
     */
    public static class ToolLimit {

        /**
         * Maximum ids accepted per tool call.
         */
        public static final int MAX_IDS = 50;

        /**
         * Maximum history records returned per tool call.
         */
        public static final int MAX_HISTORY_RECORDS = 200;

        /**
         * Maximum agentic loop rounds per conversation turn.
         */
        public static final int MAX_AGENT_LOOP_ROUNDS = 8;

        private ToolLimit() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }
    }

    /**
     * Shared agentic tool messages.
     *
     * @author pnoker
     * @since 2016.10.1
     */
    public static class ToolMessage {

        /**
         * Tool message when status/health tools are unavailable in the deployment mode.
         */
        public static final String STATUS_HEALTH_UNAVAILABLE =
                "Status and health tools are not available in this deployment mode.";

        /**
         * Tool message when profile tools are unavailable in the deployment mode.
         */
        public static final String PROFILE_UNAVAILABLE = "Profile tools are not available in this deployment mode.";

        /**
         * Tool message when the system health snapshot is unavailable.
         */
        public static final String SYSTEM_HEALTH_UNAVAILABLE = "System health snapshot is unavailable.";

        /**
         * Generic tool request failure message.
         */
        public static final String REQUEST_FAILED = "Request failed";

        private ToolMessage() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }
    }
}
