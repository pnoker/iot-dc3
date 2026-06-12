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

import java.util.List;

/**
 * MCP and OAuth protocol constants shared by auth, gateway, and web security
 * modules.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
public class McpConstant {

    public static final String URL_PREFIX = "/mcp";

    public static final String INTERNAL_URL_PREFIX = URL_PREFIX + "/internal";

    public static final String WELL_KNOWN_AUTHORIZATION_SERVER = "/.well-known/oauth-authorization-server";

    public static final String WELL_KNOWN_PROTECTED_RESOURCE = "/.well-known/oauth-protected-resource";

    public static final String OAUTH2_URL_PREFIX = "/oauth2";

    public static final String OAUTH2_AUTHORIZE = OAUTH2_URL_PREFIX + "/authorize";

    public static final String OAUTH2_INTROSPECT = OAUTH2_URL_PREFIX + "/introspect";

    public static final String OAUTH2_JWKS = OAUTH2_URL_PREFIX + "/jwks";

    public static final String OAUTH2_REGISTER = OAUTH2_URL_PREFIX + "/register";

    public static final String OAUTH2_REVOKE = OAUTH2_URL_PREFIX + "/revoke";

    public static final String OAUTH2_TOKEN = OAUTH2_URL_PREFIX + "/token";

    public static final String INTERNAL_AUDIT = INTERNAL_URL_PREFIX + "/audit";

    public static final String INTERNAL_CATALOG_REFRESH = INTERNAL_URL_PREFIX + "/catalog/refresh";

    public static final String INTERNAL_TOOLS_LIST = INTERNAL_URL_PREFIX + "/tools/list";

    public static final String INTERNAL_TOOLS_RESOLVE = INTERNAL_URL_PREFIX + "/tools/resolve";

    private McpConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    /**
     * OAuth metadata, client, grant, and token constants.
     */
    public static class OAuth {

        public static final String AUTH_METHOD_CLIENT_SECRET_BASIC = "client_secret_basic";

        public static final String AUTH_METHOD_CLIENT_SECRET_POST = "client_secret_post";

        public static final String AUTH_METHOD_NONE = "none";

        public static final String CLIENT_ID_PREFIX = "dc3_";

        public static final String CLIENT_TYPE_CONFIDENTIAL = "CONFIDENTIAL";

        public static final String CLIENT_TYPE_PUBLIC = "PUBLIC";

        public static final String CODE_CHALLENGE_METHOD_S256 = "S256";

        public static final String GRANT_AUTHORIZATION_CODE = "authorization_code";

        public static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";

        public static final String GRANT_REFRESH_TOKEN = "refresh_token";

        public static final String RESPONSE_TYPE_CODE = "code";

        public static final String TOKEN_TYPE_BEARER = "Bearer";

        private OAuth() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * MCP OAuth scopes.
     */
    public static class Scope {

        public static final String RESOURCES_READ = "mcp:resources:read";

        public static final String TOOLS_CALL = "mcp:tools:call";

        public static final String TOOLS_CALL_HIGH = "mcp:tools:call:high";

        public static final String TOOLS_LIST = "mcp:tools:list";

        public static final List<String> SUPPORTED = List.of(TOOLS_LIST, TOOLS_CALL, TOOLS_CALL_HIGH, RESOURCES_READ);

        private Scope() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * JSON-RPC protocol constants used by the public MCP endpoint.
     */
    public static class JsonRpc {

        public static final String FIELD_ERROR = "error";

        public static final String FIELD_ID = "id";

        public static final String FIELD_JSONRPC = "jsonrpc";

        public static final String FIELD_METHOD = "method";

        public static final String FIELD_RESULT = "result";

        public static final String ERROR_FIELD_CODE = "code";

        public static final String ERROR_FIELD_MESSAGE = "message";

        public static final String METHOD_INITIALIZE = "initialize";

        public static final String METHOD_NOTIFICATIONS_INITIALIZED = "notifications/initialized";

        public static final String METHOD_PING = "ping";

        public static final String METHOD_TOOLS_CALL = "tools/call";

        public static final String METHOD_TOOLS_LIST = "tools/list";

        public static final String VERSION = "2.0";

        public static final int ERROR_INTERNAL = -32000;

        public static final int ERROR_METHOD_NOT_FOUND = -32601;

        private JsonRpc() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Shared request/response field names for gateway-auth MCP runtime calls.
     */
    public static class Field {

        public static final String ACTIVE = "active";

        public static final String ACCESS_TOKEN = "access_token";

        public static final String ARGUMENT_DIGEST = "argumentDigest";

        public static final String ARGUMENTS = "arguments";

        public static final String AUD = "aud";

        public static final String CLIENT_ID = "client_id";

        public static final String CLIENT_ID_CAMEL = "clientId";

        public static final String CLIENT_NAME = "clientName";

        public static final String CLIENT_VERSION = "clientVersion";

        public static final String CODE_CHALLENGE = "code_challenge";

        public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";

        public static final String CONFIRM_ID = "confirmId";

        public static final String CONFIRM_ID_META = "confirm_id";

        public static final String CONNECTION_ID = "connectionId";

        public static final String CONNECTION_ID_REQUEST = "connection_id";

        public static final String DISPLAY_NAME = "display_name";

        public static final String DURATION_MS = "durationMs";

        public static final String ERROR = "error";

        public static final String ERROR_CODE = "errorCode";

        public static final String ERROR_DESCRIPTION = "error_description";

        public static final String EXP = "exp";

        public static final String EXPIRES_IN = "expires_in";

        public static final String GRANT_TYPE = "grant_type";

        public static final String IAT = "iat";

        public static final String IDEMPOTENCY_KEY = "idempotencyKey";

        public static final String IDEMPOTENCY_KEY_META = "idempotency_key";

        public static final String ISS = "iss";

        public static final String JTI = "jti";

        public static final String KEYWORD = "keyword";

        public static final String LIMIT = "limit";

        public static final String MCP_CONNECTION_ID = "mcp_connection_id";

        public static final String META = "_meta";

        public static final String NAME = "name";

        public static final String PARAMS = "params";

        public static final String PERMISSION_CODE = "permissionCode";

        public static final String PERMISSION_CODE_META = "permission_code";

        public static final String PRINCIPAL_ID = "principal_id";

        public static final String PRINCIPAL_ID_CAMEL = "principalId";

        public static final String PRINCIPAL_NAME = "principal_name";

        public static final String PRINCIPAL_TYPE = "principal_type";

        public static final String PRINCIPAL_TYPE_CAMEL = "principalType";

        public static final String REDIRECT_URI = "redirect_uri";

        public static final String REFRESH_TOKEN = "refresh_token";

        public static final String REMOTE_IP = "remoteIp";

        public static final String RISK_LEVEL = "riskLevel";

        public static final String RISK_LEVEL_META = "risk_level";

        public static final String SCOPE = "scope";

        public static final String STATUS = "status";

        public static final String SUB = "sub";

        public static final String TENANT_ID = "tenant_id";

        public static final String TENANT_ID_CAMEL = "tenantId";

        public static final String TOKEN = "token";

        public static final String TOKEN_TYPE = "token_type";

        public static final String TOOL_ID = "toolId";

        public static final String TOOL_ID_META = "tool_id";

        public static final String TOOL_IDS = "tool_ids";

        public static final String TOOL_NAME = "toolName";

        public static final String TOOL_NAME_REQUEST = "tool_name";

        public static final String TOOLS = "tools";

        public static final String TRACE_ID = "traceId";

        private Field() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * MCP tool call response fields.
     */
    public static class ToolResult {

        public static final String CONTENT = "content";

        public static final String IS_ERROR = "isError";

        public static final String TEXT = "text";

        public static final String TYPE = "type";

        public static final String TYPE_TEXT = "text";

        private ToolResult() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Public MCP server metadata and capability fields.
     */
    public static class Server {

        public static final String BEARER_METHOD_HEADER = "header";

        public static final String CAPABILITY_LIST_CHANGED = "listChanged";

        public static final String CAPABILITY_TOOLS = "tools";

        public static final String NAME = "iot-dc3-gateway";

        public static final String PROTOCOL_VERSION = "2025-06-18";

        public static final String VERSION = "2026.5.22";

        private Server() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * MCP risk levels stored in the tool catalog.
     */
    public static class RiskLevel {

        public static final String HIGH = "HIGH";

        public static final String LOW = "LOW";

        public static final String MEDIUM = "MEDIUM";

        private RiskLevel() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Audit statuses emitted by the gateway runtime.
     */
    public static class Audit {

        public static final String DENIED = "DENIED";

        public static final String ERROR = "ERROR";

        public static final String POLICY_DENIED = "POLICY_DENIED";

        public static final String SUCCESS = "SUCCESS";

        public static final String UNKNOWN = "UNKNOWN";

        private Audit() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

}
