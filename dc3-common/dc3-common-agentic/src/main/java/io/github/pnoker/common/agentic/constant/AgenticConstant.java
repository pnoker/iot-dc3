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
package io.github.pnoker.common.agentic.constant;

/**
 * Agentic service constants.
 */
public class AgenticConstant {

    public static final String SERVICE_NAME = "dc3-center-agentic";

    /**
     * URL prefix for the chat REST API.
     * Gateway: /api/v3/agentic/chat/** → agentic:8600/agentic/chat/**
     */
    public static final String CHAT_URL_PREFIX = "/chat";

    /**
     * URL prefix for the session REST API.
     */
    public static final String SESSION_URL_PREFIX = "/session";

    private AgenticConstant() {
    }
}
