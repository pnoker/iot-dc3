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

package com.pnoker.api.dbs.user.feign;

import com.pnoker.api.dbs.user.hystrix.UserDbsFeignApiHystrix;
import com.pnoker.common.model.User;
import com.pnoker.common.utils.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "DC3-DBS", fallbackFactory = UserDbsFeignApiHystrix.class)
@RequestMapping(value = "/api/v3/dbs/user")
public interface UserDbsFeignApi {
    /**
     * 通过用户名查询用户
     *
     * @param username 用户名
     * @return R
     */
    @GetMapping("/name/{username}")
    Response<User> user(@PathVariable("username") String username);
}
