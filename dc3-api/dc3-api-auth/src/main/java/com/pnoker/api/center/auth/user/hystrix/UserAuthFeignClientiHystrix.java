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

package com.pnoker.api.center.auth.user.hystrix;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.auth.user.feign.UserAuthFeignClient;
import com.pnoker.common.bean.Response;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.User;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>UserAuthFeignClientiHystrix
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Component
public class UserAuthFeignClientiHystrix implements FallbackFactory<UserAuthFeignClient> {

    @Override
    public UserAuthFeignClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-AUTH" : throwable.getMessage();
        log.error("UserAuthFeignClient:{},hystrix服务降级处理", message, throwable);

        return new UserAuthFeignClient() {

            @Override
            public Response<User> add(User user) {
                return Response.fail(message);
            }

            @Override
            public Response<Boolean> delete(Long id) {
                return Response.fail(message);
            }

            @Override
            public Response<User> update(User user) {
                return Response.fail(message);
            }

            @Override
            public Response<User> selectById(Long id) {
                return Response.fail(message);
            }

            @Override
            public Response<Page<User>> list(UserDto userDto) {
                return Response.fail(message);
            }

            @Override
            public Response<Boolean> checkUserExist(String username) {
                return Response.fail(message);
            }
        };
    }
}