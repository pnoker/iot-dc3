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

package com.pnoker.api.center.auth.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.auth.hystrix.AuthFeignApiHystrix;
import com.pnoker.common.bean.Response;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.Token;
import com.pnoker.common.entity.auth.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@FeignClient(path = Common.Service.DC3_AUTH_URL_PREFIX, name = Common.Service.DC3_AUTH, fallbackFactory = AuthFeignApiHystrix.class)
public interface AuthFeignClient {

    /**
     * 新增 新增 User 记录
     *
     * @param user
     * @return true/false
     */
    @PostMapping("/user/add")
    Response<Long> add(@Validated @RequestBody User user);

    /**
     * 删除 根据 ID 删除 User
     *
     * @param id userId
     * @return true/false
     */
    @PostMapping("/user/delete/{id}")
    Response<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 修改 User 记录
     *
     * @param user
     * @return true/false
     */
    @PostMapping("/user/update")
    Response<Boolean> update(@RequestBody User user);

    /**
     * 查询 根据ID查询 User
     *
     * @param id
     * @return user
     */
    @GetMapping("/user/id/{id}")
    Response<User> selectById(@PathVariable(value = "id") Long id);

    /**
     * 分页查询 User
     *
     * @param userDto
     * @return userList
     */
    @PostMapping("/user/list")
    Response<Page<User>> list(@RequestBody(required = false) UserDto userDto);

    /**
     * 检测用户是否存在
     *
     * @param username
     * @return true/false
     */
    @GetMapping("/check/{username}")
    Response<Boolean> checkUserExist(@PathVariable(value = "username") String username);

    /**
     * 获取Token
     *
     * @param user
     * @return true/false
     */
    @PostMapping("/token")
    Response<TokenDto> generateToken(@Validated @RequestBody User user);

    /**
     * 检测Token是否有效
     *
     * @param token
     * @return true/false
     */
    @PostMapping("/check/token")
    Response<Boolean> checkTokenValid(@Validated @RequestBody Token token);

}
