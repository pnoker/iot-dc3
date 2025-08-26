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
 * 基础 Controller 类接口
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface BaseController {

    // 默认方法

    /**
     * 获取租户ID
     *
     * @return 租户ID
     */
    default Mono<RequestHeader.UserHeader> getUserHeader() {
        return UserHeaderUtil.getUserHeader();
    }

    /**
     * 获取租户ID
     *
     * @return 租户ID
     */
    default Mono<Long> getTenantId() {
        return UserHeaderUtil.getTenantId();
    }

    /**
     * 获取用户ID
     *
     * @return User ID
     */
    default Mono<Long> getUserId() {
        return UserHeaderUtil.getUserId();
    }

    /**
     * 获取用户昵称
     *
     * @return Nick Name
     */
    default Mono<String> getNickName() {
        return UserHeaderUtil.getNickName();
    }

    /**
     * 获取用户名
     *
     * @return User Name
     */
    default Mono<String> getUserName() {
        return UserHeaderUtil.getUserName();
    }
}
