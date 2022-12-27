/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.service;

import io.github.pnoker.api.center.auth.dto.UserExtDto;
import io.github.pnoker.common.base.Service;
import io.github.pnoker.common.entity.UserExt;

/**
 * UserExt Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface UserExtService extends Service<UserExt, UserExtDto> {

    /**
     * 根据用户名查询用户
     *
     * @param userName 用户名称
     * @param isEx     Throw Exception
     * @return UserExt
     */
    UserExt selectByUserName(String userName, boolean isEx);

    /**
     * 根据手机号查询用户
     *
     * @param phone Phone
     * @param isEx  Throw Exception
     * @return UserExt
     */
    UserExt selectByPhone(String phone, boolean isEx);

    /**
     * 根据邮箱查询用户
     *
     * @param email Email
     * @param isEx  Throw Exception
     * @return UserExt
     */
    UserExt selectByEmail(String email, boolean isEx);
}
