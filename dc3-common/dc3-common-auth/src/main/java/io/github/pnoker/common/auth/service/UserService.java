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

package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.query.UserQuery;
import io.github.pnoker.common.base.service.BaseService;

/**
 * User Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface UserService extends BaseService<UserBO, UserQuery> {

    /**
     * 根据用户名称查询用户
     *
     * @param userName       用户名称
     * @param throwException Throw Exception
     * @return User
     */
    UserBO selectByUserName(String userName, boolean throwException);

    /**
     * 根据手机号查询用户
     *
     * @param phone          Phone
     * @param throwException Throw Exception
     * @return User
     */
    UserBO selectByPhone(String phone, boolean throwException);

    /**
     * 根据邮箱查询用户
     *
     * @param email          Email
     * @param throwException Throw Exception
     * @return User
     */
    UserBO selectByEmail(String email, boolean throwException);
}
