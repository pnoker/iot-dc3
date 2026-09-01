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
import java.util.Map;

/**
 * MCP and OAuth protocol constants shared by auth, gateway, and web security
 * modules.
 *
 * @author pnoker
 * @since 2026.6.12
 */
public class McpConstant {

    /**
     * Base URL prefix of the MCP endpoint.
     */
    public static final String URL_PREFIX = "/mcp";

    /**
     * Well-known URI advertising the OAuth authorization server metadata.
     */
    public static final String WELL_KNOWN_AUTHORIZATION_SERVER = "/.well-known/oauth-authorization-server";

    /**
     * Well-known URI advertising the OAuth protected-resource metadata.
     */
    public static final String WELL_KNOWN_PROTECTED_RESOURCE = "/.well-known/oauth-protected-resource";

    /**
     * Base URL prefix of the OAuth2 endpoints.
     */
    public static final String OAUTH2_URL_PREFIX = "/oauth2";

    /**
     * OAuth2 authorization endpoint path.
     */
    public static final String OAUTH2_AUTHORIZE = OAUTH2_URL_PREFIX + "/authorize";

    /**
     * OAuth2 JWKS endpoint path.
     */
    public static final String OAUTH2_JWKS = OAUTH2_URL_PREFIX + "/jwks";

    /**
     * OAuth2 dynamic client registration endpoint path.
     */
    public static final String OAUTH2_REGISTER = OAUTH2_URL_PREFIX + "/register";

    /**
     * OAuth2 token revocation endpoint path.
     */
    public static final String OAUTH2_REVOKE = OAUTH2_URL_PREFIX + "/revoke";

    /**
     * OAuth2 token endpoint path.
     */
    public static final String OAUTH2_TOKEN = OAUTH2_URL_PREFIX + "/token";

    private McpConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    /**
     * OAuth metadata, client, grant, and token constants.
     */
    public static class OAuth {

        /**
         * Client authentication method: client_secret_basic.
         */
        public static final String AUTH_METHOD_CLIENT_SECRET_BASIC = "client_secret_basic";

        /**
         * Client authentication method: client_secret_post.
         */
        public static final String AUTH_METHOD_CLIENT_SECRET_POST = "client_secret_post";

        /**
         * Client authentication method: none (public clients).
         */
        public static final String AUTH_METHOD_NONE = "none";

        /**
         * Prefix applied to generated client ids.
         */
        public static final String CLIENT_ID_PREFIX = "dc3_";

        /**
         * Confidential client type.
         */
        public static final String CLIENT_TYPE_CONFIDENTIAL = "CONFIDENTIAL";

        /**
         * Public client type.
         */
        public static final String CLIENT_TYPE_PUBLIC = "PUBLIC";

        /**
         * PKCE code challenge method: S256.
         */
        public static final String CODE_CHALLENGE_METHOD_S256 = "S256";

        /**
         * Authorization-code grant type.
         */
        public static final String GRANT_AUTHORIZATION_CODE = "authorization_code";

        /**
         * Client-credentials grant type.
         */
        public static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";

        /**
         * Refresh-token grant type.
         */
        public static final String GRANT_REFRESH_TOKEN = "refresh_token";

        /**
         * OAuth response type: code.
         */
        public static final String RESPONSE_TYPE_CODE = "code";

        /**
         * Bearer token type.
         */
        public static final String TOKEN_TYPE_BEARER = "Bearer";

        private OAuth() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * MCP OAuth scopes.
     */
    public static class Scope {

        /**
         * Regex splitting whitespace/comma-separated scope lists.
         */
        public static final String DELIMITER_REGEX = "[\\s,]+";

        /**
         * MCP scope granting resource reads.
         */
        public static final String RESOURCES_READ = "mcp:resources:read";

        /**
         * MCP scope granting standard tool calls.
         */
        public static final String TOOLS_CALL = "mcp:tools:call";

        /**
         * MCP scope granting high-risk tool calls.
         */
        public static final String TOOLS_CALL_HIGH = "mcp:tools:call:high";

        /**
         * MCP scope granting tool listing.
         */
        public static final String TOOLS_LIST = "mcp:tools:list";

        /**
         * Scopes the platform grants and recognizes.
         */
        public static final List<String> SUPPORTED = List.of(TOOLS_LIST, TOOLS_CALL, TOOLS_CALL_HIGH, RESOURCES_READ);

        private Scope() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * JSON-RPC protocol constants used by the public MCP endpoint.
     */
    public static class JsonRpc {

        /**
         * JSON-RPC error field.
         */
        public static final String FIELD_ERROR = "error";

        /**
         * JSON-RPC request/response id field.
         */
        public static final String FIELD_ID = "id";

        /**
         * JSON-RPC protocol version field.
         */
        public static final String FIELD_JSONRPC = "jsonrpc";

        /**
         * JSON-RPC method field.
         */
        public static final String FIELD_METHOD = "method";

        /**
         * JSON-RPC result field.
         */
        public static final String FIELD_RESULT = "result";

        /**
         * JSON-RPC error code field.
         */
        public static final String ERROR_FIELD_CODE = "code";

        /**
         * JSON-RPC error message field.
         */
        public static final String ERROR_FIELD_MESSAGE = "message";

        /**
         * JSON-RPC initialize method.
         */
        public static final String METHOD_INITIALIZE = "initialize";

        /**
         * JSON-RPC notifications/initialized method.
         */
        public static final String METHOD_NOTIFICATIONS_INITIALIZED = "notifications/initialized";

        /**
         * JSON-RPC ping method.
         */
        public static final String METHOD_PING = "ping";

        /**
         * JSON-RPC tools/call method.
         */
        public static final String METHOD_TOOLS_CALL = "tools/call";

        /**
         * JSON-RPC tools/list method.
         */
        public static final String METHOD_TOOLS_LIST = "tools/list";

        /**
         * Supported MCP protocol version.
         */
        public static final String VERSION = "2.0";

        /**
         * JSON-RPC internal-error code.
         */
        public static final int ERROR_INTERNAL = -32000;

        /**
         * JSON-RPC method-not-found code.
         */
        public static final int ERROR_METHOD_NOT_FOUND = -32601;

        private JsonRpc() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Shared request/response field names for gateway-auth MCP runtime calls.
     */
    public static class Field {

        /**
         * Connection state value for active connections.
         */
        public static final String ACTIVE = "active";

        /**
         * OAuth token field: access token.
         */
        public static final String ACCESS_TOKEN = "access_token";

        /**
         * OAuth/MCP wire name for the ARGUMENT_DIGEST field.
         */
        public static final String ARGUMENT_DIGEST = "argumentDigest";

        /**
         * OAuth/MCP wire name for the ARGUMENTS field.
         */
        public static final String ARGUMENTS = "arguments";

        /**
         * JWT claim: audience.
         */
        public static final String AUD = "aud";

        /**
         * OAuth request field: client id.
         */
        public static final String CLIENT_ID = "client_id";

        /**
         * OAuth/MCP wire name for the CLIENT_ID_CAMEL field.
         */
        public static final String CLIENT_ID_CAMEL = "clientId";

        /**
         * OAuth/MCP wire name for the CLIENT_NAME_META field.
         */
        public static final String CLIENT_NAME_META = "client_name";

        /**
         * OAuth registration metadata field: client name.
         */
        public static final String CLIENT_NAME = "clientName";

        /**
         * OAuth request field: client secret.
         */
        public static final String CLIENT_SECRET = "client_secret";

        /**
         * OAuth/MCP wire name for the CLIENT_TYPE field.
         */
        public static final String CLIENT_TYPE = "client_type";

        /**
         * OAuth/MCP wire name for the CLIENT_VERSION field.
         */
        public static final String CLIENT_VERSION = "clientVersion";

        /**
         * PKCE request field: code challenge.
         */
        public static final String CODE_CHALLENGE = "code_challenge";

        /**
         * PKCE request field: code challenge method.
         */
        public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";

        /**
         * OAuth/MCP wire name for the CONFIRM_ID field.
         */
        public static final String CONFIRM_ID = "confirmId";

        /**
         * OAuth/MCP wire name for the CONFIRM_ID_META field.
         */
        public static final String CONFIRM_ID_META = "confirm_id";

        /**
         * OAuth/MCP wire name for the CONNECTION_ID field.
         */
        public static final String CONNECTION_ID = "connectionId";

        /**
         * OAuth/MCP wire name for the CONNECTION_ID_REQUEST field.
         */
        public static final String CONNECTION_ID_REQUEST = "connection_id";

        /**
         * OAuth/MCP wire name for the DISPLAY_NAME field.
         */
        public static final String DISPLAY_NAME = "display_name";

        /**
         * OAuth/MCP wire name for the DURATION_MS field.
         */
        public static final String DURATION_MS = "durationMs";

        /**
         * OAuth/MCP wire name for the ERROR field.
         */
        public static final String ERROR = "error";

        /**
         * OAuth/MCP wire name for the ERROR_CODE field.
         */
        public static final String ERROR_CODE = "errorCode";

        /**
         * OAuth/MCP wire name for the ERROR_DESCRIPTION field.
         */
        public static final String ERROR_DESCRIPTION = "error_description";

        /**
         * JWT claim: expiry time.
         */
        public static final String EXP = "exp";

        /**
         * OAuth token response field: lifetime in seconds.
         */
        public static final String EXPIRES_IN = "expires_in";

        /**
         * OAuth token request field: grant type.
         */
        public static final String GRANT_TYPE = "grant_type";

        /**
         * OAuth registration metadata field: allowed grants.
         */
        public static final String GRANT_TYPES = "grant_types";

        /**
         * JWT claim: issued-at time.
         */
        public static final String IAT = "iat";

        /**
         * OAuth/MCP wire name for the IDEMPOTENCY_KEY field.
         */
        public static final String IDEMPOTENCY_KEY = "idempotencyKey";

        /**
         * OAuth/MCP wire name for the IDEMPOTENCY_KEY_META field.
         */
        public static final String IDEMPOTENCY_KEY_META = "idempotency_key";

        /**
         * JWT claim: issuer.
         */
        public static final String ISS = "iss";

        /**
         * JWT claim: token identifier.
         */
        public static final String JTI = "jti";

        /**
         * OAuth/MCP wire name for the KEYWORD field.
         */
        public static final String KEYWORD = "keyword";

        /**
         * OAuth/MCP wire name for the LIMIT field.
         */
        public static final String LIMIT = "limit";

        /**
         * OAuth/MCP wire name for the MCP_CONNECTION_ID field.
         */
        public static final String MCP_CONNECTION_ID = "mcp_connection_id";

        /**
         * OAuth/MCP wire name for the META field.
         */
        public static final String META = "_meta";

        /**
         * OAuth/MCP wire name for the NAME field.
         */
        public static final String NAME = "name";

        /**
         * OAuth/MCP wire name for the PARAMS field.
         */
        public static final String PARAMS = "params";

        /**
         * OAuth/MCP wire name for the PERMISSION_CODE field.
         */
        public static final String PERMISSION_CODE = "permissionCode";

        /**
         * OAuth/MCP wire name for the PERMISSION_CODE_META field.
         */
        public static final String PERMISSION_CODE_META = "permission_code";

        /**
         * OAuth/MCP wire name for the PRINCIPAL_ID field.
         */
        public static final String PRINCIPAL_ID = "principal_id";

        /**
         * OAuth/MCP wire name for the PRINCIPAL_ID_CAMEL field.
         */
        public static final String PRINCIPAL_ID_CAMEL = "principalId";

        /**
         * OAuth/MCP wire name for the PRINCIPAL_NAME field.
         */
        public static final String PRINCIPAL_NAME = "principal_name";

        /**
         * OAuth/MCP wire name for the PRINCIPAL_TYPE field.
         */
        public static final String PRINCIPAL_TYPE = "principal_type";

        /**
         * OAuth/MCP wire name for the PRINCIPAL_TYPE_CAMEL field.
         */
        public static final String PRINCIPAL_TYPE_CAMEL = "principalType";

        /**
         * OAuth request field: redirect uri.
         */
        public static final String REDIRECT_URI = "redirect_uri";

        /**
         * OAuth registration metadata field: redirect uris.
         */
        public static final String REDIRECT_URIS = "redirect_uris";

        /**
         * OAuth token response field: refresh token.
         */
        public static final String REFRESH_TOKEN = "refresh_token";

        /**
         * OAuth/MCP wire name for the REMOTE_IP field.
         */
        public static final String REMOTE_IP = "remoteIp";

        /**
         * OAuth/MCP wire name for the RISK_LEVEL field.
         */
        public static final String RISK_LEVEL = "riskLevel";

        /**
         * OAuth/MCP wire name for the RISK_LEVEL_META field.
         */
        public static final String RISK_LEVEL_META = "risk_level";

        /**
         * OAuth request/response field: scopes.
         */
        public static final String SCOPE = "scope";

        /**
         * OAuth authorization request consent decision (approve or deny).
         */
        public static final String CONSENT = "consent";

        /**
         * OAuth/MCP wire name for the SERVICE_ACCOUNT_PRINCIPAL_ID field.
         */
        public static final String SERVICE_ACCOUNT_PRINCIPAL_ID = "service_account_principal_id";

        /**
         * OAuth/MCP wire name for the STATUS field.
         */
        public static final String STATUS = "status";

        /**
         * JWT claim: subject.
         */
        public static final String SUB = "sub";

        /**
         * OAuth/MCP wire name for the TENANT_ID field.
         */
        public static final String TENANT_ID = "tenant_id";

        /**
         * OAuth/MCP wire name for the TENANT_ID_CAMEL field.
         */
        public static final String TENANT_ID_CAMEL = "tenantId";

        /**
         * OAuth/MCP wire name for the TOKEN field.
         */
        public static final String TOKEN = "token";

        /**
         * OAuth/MCP wire name for the TOKEN_ENDPOINT_AUTH_METHOD field.
         */
        public static final String TOKEN_ENDPOINT_AUTH_METHOD = "token_endpoint_auth_method";

        /**
         * OAuth token response field: token type.
         */
        public static final String TOKEN_TYPE = "token_type";

        /**
         * OAuth/MCP wire name for the TOOL_ID field.
         */
        public static final String TOOL_ID = "toolId";

        /**
         * OAuth/MCP wire name for the TOOL_ID_META field.
         */
        public static final String TOOL_ID_META = "tool_id";

        /**
         * OAuth/MCP wire name for the TOOL_IDS field.
         */
        public static final String TOOL_IDS = "tool_ids";

        /**
         * OAuth/MCP wire name for the TOOL_NAME field.
         */
        public static final String TOOL_NAME = "toolName";

        /**
         * OAuth/MCP wire name for the TOOL_NAME_REQUEST field.
         */
        public static final String TOOL_NAME_REQUEST = "tool_name";

        /**
         * OAuth/MCP wire name for the TOOLS field.
         */
        public static final String TOOLS = "tools";

        /**
         * OAuth/MCP wire name for the TRACE_ID field.
         */
        public static final String TRACE_ID = "traceId";

        private Field() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * MCP tool definition fields exposed by the public JSON-RPC tools/list
     * method.
     */
    public static class ToolDefinition {

        /**
         * OAuth/MCP wire name for the ADDITIONAL_PROPERTIES field.
         */
        public static final String ADDITIONAL_PROPERTIES = "additionalProperties";

        /**
         * OAuth/MCP wire name for the ANNOTATIONS field.
         */
        public static final String ANNOTATIONS = "annotations";

        /**
         * OAuth/MCP wire name for the DESTRUCTIVE_HINT field.
         */
        public static final String DESTRUCTIVE_HINT = "destructiveHint";

        /**
         * OAuth/MCP wire name for the DESCRIPTION field.
         */
        public static final String DESCRIPTION = "description";

        /**
         * OAuth/MCP wire name for the IDEMPOTENT_HINT field.
         */
        public static final String IDEMPOTENT_HINT = "idempotentHint";

        /**
         * OAuth/MCP wire name for the INPUT_SCHEMA field.
         */
        public static final String INPUT_SCHEMA = "inputSchema";

        /**
         * OAuth/MCP wire name for the OPEN_WORLD_HINT field.
         */
        public static final String OPEN_WORLD_HINT = "openWorldHint";

        /**
         * OAuth/MCP wire name for the READ_ONLY_HINT field.
         */
        public static final String READ_ONLY_HINT = "readOnlyHint";

        /**
         * OAuth/MCP wire name for the TITLE field.
         */
        public static final String TITLE = "title";

        /**
         * OAuth/MCP wire name for the TYPE field.
         */
        public static final String TYPE = "type";

        /**
         * OAuth/MCP wire name for the TYPE_OBJECT field.
         */
        public static final String TYPE_OBJECT = "object";

        /**
         * default input schema constant.
         */
        public static final Map<String, Object> DEFAULT_INPUT_SCHEMA = Map.of(
                TYPE, TYPE_OBJECT,
                ADDITIONAL_PROPERTIES, true
        );

        private ToolDefinition() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * MCP tool call response fields.
     */
    public static class ToolResult {

        /**
         * OAuth/MCP wire name for the CONTENT field.
         */
        public static final String CONTENT = "content";

        /**
         * OAuth/MCP wire name for the IS_ERROR field.
         */
        public static final String IS_ERROR = "isError";

        /**
         * OAuth/MCP wire name for the TEXT field.
         */
        public static final String TEXT = "text";

        /**
         * OAuth/MCP wire name for the TYPE field.
         */
        public static final String TYPE = "type";

        /**
         * OAuth/MCP wire name for the TYPE_TEXT field.
         */
        public static final String TYPE_TEXT = "text";

        private ToolResult() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Public MCP server metadata and capability fields.
     */
    public static class Server {

        /**
         * OAuth/MCP wire name for the BEARER_METHOD_HEADER field.
         */
        public static final String BEARER_METHOD_HEADER = "header";

        /**
         * OAuth/MCP wire name for the CAPABILITY_LIST_CHANGED field.
         */
        public static final String CAPABILITY_LIST_CHANGED = "listChanged";

        /**
         * OAuth/MCP wire name for the CAPABILITY_TOOLS field.
         */
        public static final String CAPABILITY_TOOLS = "tools";

        /**
         * OAuth/MCP wire name for the NAME field.
         */
        public static final String NAME = "iot-dc3-gateway";

        /**
         * OAuth/MCP wire name for the PROTOCOL_VERSION field.
         */
        public static final String PROTOCOL_VERSION = "2025-06-18";

        /**
         * Supported MCP protocol version.
         */
        public static final String VERSION = "2026.5.22";

        private Server() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * MCP risk levels stored in the tool catalog.
     */
    public static class RiskLevel {

        /**
         * OAuth/MCP wire name for the HIGH field.
         */
        public static final String HIGH = "HIGH";

        /**
         * OAuth/MCP wire name for the LOW field.
         */
        public static final String LOW = "LOW";

        /**
         * OAuth/MCP wire name for the MEDIUM field.
         */
        public static final String MEDIUM = "MEDIUM";

        private RiskLevel() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * High-risk tool call confirmation ticket constants.
     */
    public static class Confirmation {

        /**
         * OAuth/MCP wire name for the DECISION_AUTHORIZED field.
         */
        public static final String DECISION_AUTHORIZED = "AUTHORIZED";

        /**
         * OAuth/MCP wire name for the DECISION_CONFIRM_REQUIRED field.
         */
        public static final String DECISION_CONFIRM_REQUIRED = "CONFIRM_REQUIRED";

        /**
         * OAuth/MCP wire name for the DECISION_REJECTED field.
         */
        public static final String DECISION_REJECTED = "REJECTED";

        /**
         * OAuth/MCP wire name for the STATUS_PENDING field.
         */
        public static final String STATUS_PENDING = "PENDING";

        /**
         * OAuth/MCP wire name for the STATUS_CONSUMED field.
         */
        public static final String STATUS_CONSUMED = "CONSUMED";

        private Confirmation() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Audit statuses emitted by the gateway runtime.
     */
    public static class Audit {

        /**
         * OAuth/MCP wire name for the DENIED field.
         */
        public static final String DENIED = "DENIED";

        /**
         * OAuth/MCP wire name for the ERROR field.
         */
        public static final String ERROR = "ERROR";

        /**
         * OAuth/MCP wire name for the POLICY_DENIED field.
         */
        public static final String POLICY_DENIED = "POLICY_DENIED";

        /**
         * OAuth/MCP wire name for the SUCCESS field.
         */
        public static final String SUCCESS = "SUCCESS";

        /**
         * OAuth/MCP wire name for the UNKNOWN field.
         */
        public static final String UNKNOWN = "UNKNOWN";

        private Audit() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

}
