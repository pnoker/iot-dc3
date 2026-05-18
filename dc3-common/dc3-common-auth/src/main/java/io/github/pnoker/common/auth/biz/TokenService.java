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

package io.github.pnoker.common.auth.biz;

import io.github.pnoker.common.auth.entity.bean.TokenValid;

/**
 * Token Interface
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface TokenService {

    /**
     * @param loginName  Name
     * @param tenantCode TenantCode
     * @return R of String
     */
    String generateSalt(String loginName, String tenantCode);

    /**
     * @param loginName  Name
     * @param salt       User Salt
     * @param password   User Password
     * @param tenantCode TenantCode
     * @return R of String
     */
    String generateToken(String loginName, String salt, String password, String tenantCode);

    /**
     * @param loginName  Name
     * @param salt
     * @param token      Token
     * @param tenantCode TenantCode
     * @return TokenValid
     */
    TokenValid checkValid(String loginName, String salt, String token, String tenantCode);

    /**
     * Attempt to acknowledge a client-initiated logout. The current token implementation
     * is stateless JWT, so server-side state does not need to change; this hook lets
     * us record the logout event and gives us a single seam for future token
     * revocation (denylist, refresh-token cleanup, etc.).
     * <p>
     * Returns {@code false} when the tenant or login name does not resolve, since those
     * are normal "nothing-to-cancel" outcomes rather than exceptional states.
     *
     * @param loginName  login name
     * @param tenantCode tenant code
     * @return {@code true} when the logout was accepted, {@code false} when there was
     * no matching tenant or user to cancel
     */
    boolean tryCancelToken(String loginName, String tenantCode);

}
