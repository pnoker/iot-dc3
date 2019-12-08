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

package com.pnoker.dbs.api.user.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.base.bean.Response;
import com.pnoker.common.base.dto.UserDto;
import com.pnoker.common.base.model.User;
import com.pnoker.dbs.api.user.hystrix.UserDbsFeignHystrix;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * <p>User 数据 UserClient
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@RequestMapping("/api/v3/dbs/user")
@FeignClient(name = "DC3-DBS", fallbackFactory = UserDbsFeignHystrix.class)
public interface UserDbsFeignClient {

    /**
     * 新增 新增 User 记录
     *
     * @param user
     * @return true/false
     */
    @PostMapping("/add")
    Response<Long> add(@RequestBody User user);

    /**
     * 删除 根据 ID 删除 User
     *
     * @param id userId
     * @return true/false
     */
    @PostMapping("/delete/{id}")
    Response<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 修改 User 记录
     *
     * @param user
     * @return true/false
     */
    @PostMapping("/update")
    Response<Boolean> update(@RequestBody User user);

    /**
     * 查询 根据ID查询 User
     *
     * @param id
     * @return user
     */
    @GetMapping("/id/{id}")
    Response<User> selectById(@PathVariable(value = "id") Long id);

    /**
     * 分页查询 User
     *
     * @param userDto
     * @return rtmpList
     */
    @PostMapping("/list")
    Response<Page<User>> list(@RequestBody(required = false) UserDto userDto);

    /**
     * 通过用户名查询用户
     *
     * @param username 用户名
     * @return user
     */
    @GetMapping("/username/{username}")
    Response<User> username(@PathVariable(value = "username") String username);

    /**
     * 通过手机号查询用户
     *
     * @param phone 用户名
     * @return user
     */
    @GetMapping("/phone/{phone}")
    Response<User> phone(@PathVariable(value = "phone") String phone);

    /**
     * 通过邮箱查询用户
     *
     * @param email 用户名
     * @return user
     */
    @GetMapping("/email/{email}")
    Response<User> email(@PathVariable(value = "email") String email);

}
