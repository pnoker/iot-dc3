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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.dbs.user.hystrix.UserDbsFeignApiHystrix;
import com.pnoker.common.dto.UserDto;
import com.pnoker.common.model.User;
import com.pnoker.common.bean.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@RequestMapping("/api/v3/dbs/user")
@FeignClient(name = "DC3-DBS", fallbackFactory = UserDbsFeignApiHystrix.class)
public interface UserDbsFeignApi {

    /**
     * 新增 新增 User 记录
     *
     * @param user
     * @return true/false
     */
    @PostMapping("/add")
    Response<Long> add(User user);

    /**
     * 删除 根据 ID 删除 User
     *
     * @param id userId
     * @return true/false
     */
    @PostMapping("/delete/{id}")
    Response<Boolean> delete(Long id);

    /**
     * 修改 修改 User 记录
     *
     * @param user
     * @return true/false
     */
    @PostMapping("/update")
    Response<Boolean> update(User user);

    /**
     * 查询 根据ID查询 User
     *
     * @param id
     * @return user
     */
    @GetMapping("/id/{id}")
    Response<User> selectById(Long id);

    /**
     * 通过用户名查询用户
     *
     * @param username 用户名
     * @return user
     */
    @GetMapping("/username/{username}")
    Response<User> username(String username);

    /**
     * 通过手机号查询用户
     *
     * @param phone 用户名
     * @return user
     */
    @GetMapping("/phone/{phone}")
    Response<User> phone(String phone);

    /**
     * 通过邮箱查询用户
     *
     * @param email 用户名
     * @return user
     */
    @GetMapping("/email/{email}")
    Response<User> email(String email);

    /**
     * 分页查询 User
     *
     * @param userDto
     * @return rtmpList
     */
    @PostMapping("/list")
    Response<Page<User>> list(UserDto userDto);
}
