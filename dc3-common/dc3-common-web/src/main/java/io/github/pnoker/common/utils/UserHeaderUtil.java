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

package io.github.pnoker.common.utils;

import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 用户请求头 相关工具类
 *
 * @author pnoker
 * @version 2025.6.1
 * @since 2022.1.0
 */
@Slf4j
public class UserHeaderUtil {

    private UserHeaderUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 获取用户请求头信息
     *
     * @return 用户信息
     */
    public static Mono<RequestHeader.UserHeader> getUserHeader() {
        return Mono.deferContextual(context -> {
            if (!context.hasKey(RequestConstant.Key.USER_HEADER)) {
                return Mono.error(new UnAuthorizedException("Unable to get user header"));
            }
            RequestHeader.UserHeader userHeader = context.get(RequestConstant.Key.USER_HEADER);
            return Mono.just(userHeader);
        });
    }

    /**
     * 获取用户请求头的租户ID
     *
     * @return 租户ID
     */
    public static Mono<Long> getTenantId() {
        return getUserHeader().flatMap(userHeader -> {
            Long tenantId = userHeader.getTenantId();
            if (Objects.isNull(tenantId)) {
                return Mono.error(new UnAuthorizedException("Unable to get tenant id of user header"));
            }
            return Mono.just(tenantId);
        });
    }

    /**
     * 获取用户请求头的用户ID
     *
     * @return 用户ID
     */
    public static Mono<Long> getUserId() {
        return getUserHeader().flatMap(userHeader -> {
            Long userId = userHeader.getUserId();
            if (Objects.isNull(userId)) {
                return Mono.error(new UnAuthorizedException("Unable to get user id of user header"));
            }
            return Mono.just(userId);
        });
    }

    /**
     * 获取用户请求头的用户昵称
     *
     * @return 用户昵称
     */
    public static Mono<String> getNickName() {
        return getUserHeader().flatMap(userHeader -> {
            String nickName = userHeader.getNickName();
            if (CharSequenceUtil.isEmpty(nickName)) {
                return Mono.error(new UnAuthorizedException("Unable to get nick name of user header"));
            }
            return Mono.just(nickName);
        });
    }

    /**
     * 获取用户请求头的用户名称
     *
     * @return 用户名称
     */
    public static Mono<String> getUserName() {
        return getUserHeader().flatMap(userHeader -> {
            String userName = userHeader.getUserName();
            if (CharSequenceUtil.isEmpty(userName)) {
                return Mono.error(new UnAuthorizedException("Unable to get user name of user header"));
            }
            return Mono.just(userName);
        });
    }

}
