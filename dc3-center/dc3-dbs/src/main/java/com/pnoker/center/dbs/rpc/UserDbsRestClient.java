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

package com.pnoker.center.dbs.rpc;

import com.pnoker.api.dbs.user.feign.UserDbsFeignApi;
import com.pnoker.center.dbs.service.UserService;
import com.pnoker.common.model.User;
import com.pnoker.common.utils.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>user dbs rest client
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v3/dbs/user")
public class UserDbsRestClient implements UserDbsFeignApi {
    @Autowired
    private UserService userService;

    @Override
    @GetMapping("/name/{username}")
    public Response<User> user(@PathVariable("username") String username) {
        User user = userService.selectByUsername(username);
        return null != user ? Response.ok(user) : Response.fail("username does not exist");
    }
}
