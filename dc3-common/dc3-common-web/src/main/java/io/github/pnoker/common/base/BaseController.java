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

package io.github.pnoker.common.base;

import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.utils.UserHeaderUtil;
import reactor.core.publisher.Mono;

/**
 * Base Controller Interface
 * <p>
 * Provides default methods for common controller operations such as extracting user
 * information from request headers in reactive applications.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface BaseController {

    /**
     * Get user header information from request context
     *
     * @return User header information as Mono
     */
    default Mono<RequestHeader.UserHeader> getUserHeader() {
        return UserHeaderUtil.getUserHeader();
    }

    /**
     * Get tenant ID from user header
     *
     * @return Tenant ID as Mono
     */
    default Mono<Long> getTenantId() {
        return UserHeaderUtil.getTenantId();
    }

    /**
     * Get user ID from user header
     *
     * @return User ID as Mono
     */
    default Mono<Long> getUserId() {
        return UserHeaderUtil.getUserId();
    }

    /**
     * Get user nickname from user header
     *
     * @return User nickname as Mono
     */
    default Mono<String> getNickName() {
        return UserHeaderUtil.getNickName();
    }

    /**
     * Get username from user header
     *
     * @return Username as Mono
     */
    default Mono<String> getUserName() {
        return UserHeaderUtil.getUserName();
    }

}
