/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.dbs.api.user.hystrix;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.bean.Response;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.User;
import com.pnoker.dbs.api.user.feign.UserDbsFeignClient;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>熔断
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Component
public class UserDbsFeignHystrix implements FallbackFactory<UserDbsFeignClient> {

    @Override
    public UserDbsFeignClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-DBS" : throwable.getMessage();
        log.error("UserFeignApi失败:{},hystrix服务降级处理", message, throwable);
        return new UserDbsFeignClient() {
            @Override
            public Response<Long> add(User user) {
                return Response.fail(message);
            }

            @Override
            public Response<Boolean> delete(Long id) {
                return Response.fail(message);
            }

            @Override
            public Response<Boolean> update(User user) {
                return Response.fail(message);
            }

            @Override
            public Response<User> selectById(Long id) {
                return Response.fail(message);
            }

            @Override
            public Response<User> username(String username) {
                return Response.fail(message);
            }

            @Override
            public Response<User> phone(String phone) {
                return Response.fail(message);
            }

            @Override
            public Response<User> email(String email) {
                return Response.fail(message);
            }

            @Override
            public Response<Page<User>> list(UserDto userDto) {
                return Response.fail(message);
            }
        };
    }
}
