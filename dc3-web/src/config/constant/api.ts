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

/**
 * Gateway-prefixed base paths for every `src/api/*.ts` module.
 * The gateway (dc3-gateway) strips the `/api/v3` prefix and routes the
 * remainder to the matching center service (auth / data / manager).
 *
 * Keep these in sync with:
 *   - dc3-gateway/src/main/resources/application-*.yml (StripPrefix=2)
 *   - dc3-common-constant/.../service/{Auth,Data,Manager}Constant.java
 *     (SERVICE_NAME + URL_PREFIX fragments)
 */
export const API_AUTH_BASE = 'api/v3/auth';
export const API_MCP_BASE = 'api/v3/auth/mcp';
export const API_SERVICE_ACCOUNT_BASE = 'api/v3/auth/service_account';
export const API_PRINCIPAL_BASE = 'api/v3/auth/principal';
export const API_TENANT_MEMBERSHIP_BASE = 'api/v3/auth/tenant_membership';
export const API_LOCAL_CREDENTIAL_BASE = 'api/v3/auth/local_credential';
export const API_IDENTITY_AUDIT_BASE = 'api/v3/auth/identity_audit';
export const API_AGENTIC_BASE = 'api/v3/agentic';
export const API_DATA_BASE = 'api/v3/data';
export const API_MANAGER_BASE = 'api/v3/manager';

export const MCP_SERVER_PATH = '/mcp';
