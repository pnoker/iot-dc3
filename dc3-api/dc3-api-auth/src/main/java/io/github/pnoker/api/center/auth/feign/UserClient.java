/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.api.center.auth.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.auth.dto.UserDto;
import io.github.pnoker.api.center.auth.fallback.UserClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.model.User;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;

/**
 * 用户 FeignClient
 *
 * @author pnoker
 * @since 2022.1.0
 */
@FeignClient(path = AuthServiceConstant.USER_URL_PREFIX, name = AuthServiceConstant.SERVICE_NAME, fallbackFactory = UserClientFallback.class)
public interface UserClient {

    /**
     * 新增用户
     *
     * @param user 用户
     * @return {@link io.github.pnoker.common.model.User}
     */
    @PostMapping("/add")
    R<User> add(@Validated(Insert.class) @RequestBody User user);

    /**
     * 根据 ID 删除用户
     *
     * @param id 用户ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 修改用户
     * <ol>
     * <li>支持修改: Enable,Password</li>
     * <li>不支持修改: Name</li>
     * </ol>
     *
     * @param user 用户
     * @return {@link io.github.pnoker.common.model.User}
     */
    @PostMapping("/update")
    R<User> update(@Validated(Update.class) @RequestBody User user);

    /**
     * 根据 ID 重置用户密码
     *
     * @param id 用户ID
     * @return 是否重置
     */
    @PostMapping("/reset/{id}")
    R<Boolean> restPassword(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户ID
     * @return {@link io.github.pnoker.common.model.User}
     */
    @GetMapping("/id/{id}")
    R<User> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 Name 查询 User
     *
     * @param name 用户名称
     * @return {@link io.github.pnoker.common.model.User}
     */
    @GetMapping("/name/{name}")
    R<User> selectByName(@NotNull @PathVariable(value = "name") String name);

    /**
     * 模糊分页查询 User
     *
     * @param userDto 用户和分页参数
     * @return 带分页的 {@link io.github.pnoker.common.model.User}
     */
    @PostMapping("/list")
    R<Page<User>> list(@RequestBody(required = false) UserDto userDto);

    /**
     * 检测登录名称是否有效
     *
     * @param name 用户名称
     * @return 是否有效
     */
    @GetMapping("/check/{name}")
    R<Boolean> checkLoginNameValid(@NotNull @PathVariable(value = "name") String name);

}
