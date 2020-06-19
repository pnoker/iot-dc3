/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.api.center.auth.user.hystrix;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.auth.user.feign.UserClient;
import com.dc3.common.bean.R;
import com.dc3.common.dto.UserDto;
import com.dc3.common.model.User;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * UserClientHystrix
 *
 * @author pnoker
 */
@Slf4j
@Component
public class UserClientHystrix implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-AUTH" : throwable.getMessage();
        log.error("Hystrix:{}", message);

        return new UserClient() {

            @Override
            public R<User> add(User user) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> delete(Long id) {
                return R.fail(message);
            }

            @Override
            public R<User> update(User user) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> restPassword(Long id) {
                return R.fail(message);
            }

            @Override
            public R<User> selectById(Long id) {
                return R.fail(message);
            }

            @Override
            public R<User> selectByName(String name) {
                return R.fail(message);
            }

            @Override
            public R<Page<User>> list(UserDto userDto) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> checkUserValid(String username) {
                return R.fail(message);
            }
        };
    }
}