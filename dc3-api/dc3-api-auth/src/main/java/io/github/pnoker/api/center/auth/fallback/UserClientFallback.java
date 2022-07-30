/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.api.center.auth.fallback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.auth.feign.UserClient;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.dto.UserDto;
import io.github.pnoker.common.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * UserClientFallback
 *
 * @author pnoker
 */
@Slf4j
@Component
public class UserClientFallback implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-CENTER-AUTH" : throwable.getMessage();
        log.error("Fallback:{}", message);

        return new UserClient() {

            @Override
            public R<User> add(User user) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> delete(String id) {
                return R.fail(message);
            }

            @Override
            public R<User> update(User user) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> restPassword(String id) {
                return R.fail(message);
            }

            @Override
            public R<User> selectById(String id) {
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