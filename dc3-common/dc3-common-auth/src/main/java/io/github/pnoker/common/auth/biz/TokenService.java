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
 * Token validation and management service.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface TokenService {

    /**
     * Generate a random salt for the login handshake. Validates tenant existence
     * before returning.
     *
     * @param loginName  login name
     * @param tenantCode tenant code
     * @return a random UUID salt, never null
     * @throws UnAuthorizedException if the tenant does not exist
     */
    String generateSalt(String loginName, String tenantCode);

    /**
     * Authenticate a principal and generate a signed JWT token.
     * <p>
     * Validation chain (order matters):
     * <ol>
     *   <li>Resolve and validate the tenant</li>
     *   <li>Resolve the local credential by login name</li>
     *   <li>Verify the principal is a member of the tenant</li>
     *   <li>Verify the provided password against the stored credential</li>
     *   <li>Record a failed login attempt on mismatch</li>
     *   <li>Check for password expiration</li>
     *   <li>Check for mandatory password-change flag</li>
     * </ol>
     *
     * @param loginName  login name
     * @param salt       server-issued random salt from {@link #generateSalt}
     * @param password   raw password
     * @param tenantCode tenant code
     * @return signed JWT token string
     * @throws UnAuthorizedException           on bad tenant, credential, membership, salt, or password
     * @throws PasswordChangeRequiredException when password is expired or flagged for change
     */
    String generateToken(String loginName, String salt, String password, String tenantCode);

    /**
     * Self-service password change used during login when a credential is flagged for a
     * mandatory change or has expired. Validates the tenant membership and current password
     * before storing the new password; no token is issued, the client re-authenticates after.
     *
     * @param loginName       login name
     * @param currentPassword current raw password
     * @param newPassword     new raw password
     * @param tenantCode      tenant code
     */
    void changePassword(String loginName, String currentPassword, String newPassword, String tenantCode);

    /**
     * Validate a token by resolving the tenant and credential, verifying the JWT
     * signature against the session salt, and checking the logout denylist for
     * tokens issued before a recorded logout.
     *
     * @param loginName  login name
     * @param salt       server-issued salt bound to the login session
     * @param token      JWT token string
     * @param tenantCode tenant code
     * @return token validity result carrying the validity flag and, when available,
     * the JWT expiry time; never carries principal or tenant identifiers
     * @throws UnAuthorizedException when the tenant does not resolve
     */
    TokenValid checkValid(String loginName, String salt, String token, String tenantCode);

    /**
     * Acknowledge a client-initiated logout by recording the logout instant on the
     * server-side denylist. Although the token itself is a stateless JWT, this
     * marks the principal as logged out so that any token issued before the logout
     * instant is rejected by subsequent {@link #checkValid} calls until it expires
     * naturally.
     * <p>
     * Returns {@code false} when the tenant or login name does not resolve, since those
     * are normal "nothing-to-cancel" outcomes rather than exceptional states.
     *
     * @param loginName  login name
     * @param tenantCode tenant code
     * @return {@code true} when the logout was recorded, {@code false} when there was
     * no matching tenant or user to cancel
     */
    boolean tryCancelToken(String loginName, String tenantCode);

}
