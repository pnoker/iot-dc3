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

package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.query.LocalCredentialQuery;
import io.github.pnoker.common.base.service.BaseService;

/**
 * Business service for local credentials.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
public interface LocalCredentialService extends BaseService<LocalCredentialBO, LocalCredentialQuery> {

    /**
     * Get a local credential by login name.
     *
     * @param loginName      login name to look up
     * @param throwException whether to throw {@code NotFoundException} when not found
     * @return the credential, or {@code null} when not found and throwException is false
     */
    LocalCredentialBO getByLoginName(String loginName, boolean throwException);

    boolean isLoginNameAvailable(String loginName);

    boolean verifyPassword(LocalCredentialBO credential, String rawPassword);

    void resetPassword(Long id, String rawPassword);

    /**
     * Self-service password change. Verifies the current password, stores the new password
     * hash, clears the require-password-change flag, refreshes the expiry, and resets the
     * failed-attempt and lock state. Throws when the current password does not match.
     *
     * @param loginName       login name
     * @param currentPassword current raw password
     * @param newPassword     new raw password
     */
    void changePassword(String loginName, String currentPassword, String newPassword);

    void recordSuccessfulLogin(Long id);

    void recordFailedLogin(Long id);

}
