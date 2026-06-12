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

    LocalCredentialBO getByLoginName(String loginName, boolean throwException);

    boolean isLoginNameAvailable(String loginName);

    boolean verifyPassword(LocalCredentialBO credential, String rawPassword);

    void resetPassword(Long id, String rawPassword);

    void recordSuccessfulLogin(Long id);

    void recordFailedLogin(Long id);

}
