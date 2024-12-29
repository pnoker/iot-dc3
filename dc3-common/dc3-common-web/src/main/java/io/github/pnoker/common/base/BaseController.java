/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.base;

import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.utils.UserHeaderUtil;
import reactor.core.publisher.Mono;

/**
 * 基础 Controller 类接口
 *
 * @author pnoker
 * @version 2024.3.9
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
