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

package com.pnoker.api.center.dbs.user.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.bean.Response;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.Token;
import com.pnoker.common.entity.auth.User;
import com.pnoker.api.center.dbs.user.hystrix.UserDbsFeignHystrix;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>User 数据 UserClient
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@FeignClient(path = Common.Service.DC3_DBS_USER_URL_PREFIX, name = Common.Service.DC3_DBS, fallbackFactory = UserDbsFeignHystrix.class)
public interface UserDbsFeignClient {

    /**
     * 新增 新增 User 记录
     *
     * @param user
     * @return userId
     */
    @PostMapping("/add")
    Response<Long> add(@Validated @RequestBody User user);

    /**
     * 删除 根据 ID 删除 User
     *
     * @param id
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
     * @param username
     * @return user
     */
    @GetMapping("/username/{username}")
    Response<User> username(@PathVariable(value = "username") String username);

    /**
     * 修改 修改 Token 记录
     *
     * @param token
     * @return true/false
     */
    @PostMapping("/token/update")
    Response<Boolean> updateToken(@RequestBody Token token);

    /**
     * 通过TokenId查询用户Token信息
     *
     * @param id
     * @return token
     */
    @GetMapping("/token/{id}")
    Response<Token> selectTokenById(@PathVariable(value = "id") Long id);

    /**
     * 通过TokenId查询用户Token信息
     *
     * @param appId
     * @return token
     */
    @GetMapping("/token/app/{app_id}")
    Response<Token> selectTokenByAppId(@PathVariable(value = "app_id") String appId);

}
