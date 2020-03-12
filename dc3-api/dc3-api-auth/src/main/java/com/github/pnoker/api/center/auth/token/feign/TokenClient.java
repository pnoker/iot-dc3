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

package com.github.pnoker.api.center.auth.token.feign;

import com.github.pnoker.api.center.auth.token.hystrix.TokenClientHystrix;
import com.github.pnoker.common.bean.R;
import com.github.pnoker.common.constant.Common;
import com.github.pnoker.common.model.User;
import com.github.pnoker.common.valid.Auth;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;

/**
 * <p>令牌 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_TOKEN_URL_PREFIX, name = Common.Service.DC3_AUTH, fallbackFactory = TokenClientHystrix.class)
public interface TokenClient {

    /**
     * 生成用户 Token 令牌
     *
     * @param user
     * @return TokenDto
     */
    @PostMapping
    R<String> generateToken(@Validated(Auth.class) @RequestBody User user);

    /**
     * 检测用户 Token 令牌是否有效
     *
     * @param token
     * @return Boolean
     */
    @PostMapping("/{token}")
    R<Boolean> checkTokenValid(@NotNull @PathVariable(value = "token") String token);

}
