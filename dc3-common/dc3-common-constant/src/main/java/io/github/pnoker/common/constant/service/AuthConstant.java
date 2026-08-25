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
 * Auth service related constants.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public class AuthConstant {

    /**
     * Service name registered in service discovery.
     */
    public static final String SERVICE_NAME = "dc3-center-auth";

    /**
     * URL prefix for the user controller.
     */
    public static final String USER_URL_PREFIX = "/user";

    /**
     * URL prefix for the user-profile controller.
     */
    public static final String USER_PROFILE_URL_PREFIX = "/user_profile";

    /**
     * URL prefix for the local-credential controller.
     */
    public static final String LOCAL_CREDENTIAL_URL_PREFIX = "/local_credential";

    /**
     * URL prefix for the service-account controller.
     */
    public static final String SERVICE_ACCOUNT_URL_PREFIX = "/service_account";

    /**
     * URL prefix for the principal controller.
     */
    public static final String PRINCIPAL_URL_PREFIX = "/principal";

    /**
     * URL prefix for the tenant-membership controller.
     */
    public static final String TENANT_MEMBERSHIP_URL_PREFIX = "/tenant_membership";

    /**
     * URL prefix for the identity-audit controller.
     */
    public static final String IDENTITY_AUDIT_URL_PREFIX = "/identity_audit";

    /**
     * URL prefix for the tenant controller.
     */
    public static final String TENANT_URL_PREFIX = "/tenant";

    /**
     * URL prefix for the token controller.
     */
    public static final String TOKEN_URL_PREFIX = "/token";

    /**
     * URL prefix for the dictionary controller.
     */
    public static final String DICTIONARY_URL_PREFIX = "/dictionary";

    /**
     * URL prefix for the role controller.
     */
    public static final String ROLE_URL_PREFIX = "/role";

    /**
     * URL prefix for the resource controller.
     */
    public static final String RESOURCE_URL_PREFIX = "/resource";

    /**
     * URL prefix for the role-principal bind controller.
     */
    public static final String ROLE_PRINCIPAL_URL_PREFIX = "/role_principal";

    /**
     * URL prefix for the role-resource bind controller.
     */
    public static final String ROLE_RESOURCE_URL_PREFIX = "/role_resource";

    /**
     * URL prefix for the API resource controller.
     */
    public static final String API_URL_PREFIX = "/api";

    /**
     * URL prefix for the menu controller.
     */
    public static final String MENU_URL_PREFIX = "/menu";

    /**
     * URL prefix for MCP endpoints, defined by {@link McpConstant}.
     */
    public static final String MCP_URL_PREFIX = McpConstant.URL_PREFIX;

    /**
     * dc3_resource.resource_code prefix for API leaf resources.
     */
    public static final String API_RESOURCE_CODE_PREFIX = "api:";

    /**
     * dc3_resource.resource_code prefix for virtual service grouping nodes.
     */
    public static final String API_SERVICE_NODE_CODE_PREFIX = "api:service:";

    /**
     * dc3_resource.resource_code prefix for virtual API group nodes.
     */
    public static final String API_GROUP_NODE_CODE_PREFIX = "api:group:";

    /**
     * dc3_resource.resource_code prefix for menu leaf resources.
     */
    public static final String MENU_RESOURCE_CODE_PREFIX = "menu:";

    private AuthConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}
