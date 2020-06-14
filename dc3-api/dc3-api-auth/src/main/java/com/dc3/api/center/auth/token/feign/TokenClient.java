/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.api.center.auth.token.feign;

import com.dc3.api.center.auth.token.hystrix.TokenClientHystrix;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.model.User;
import com.dc3.common.valid.Auth;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.NotNull;

/**
 * <p>令牌 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_AUTH_TOKEN_URL_PREFIX, name = Common.Service.DC3_AUTH_SERVICE_NAME, fallbackFactory = TokenClientHystrix.class)
public interface TokenClient {

    /**
     * 生成用户随机 Salt
     *
     * @param username Username
     * @return R<String>
     */
    @GetMapping("/salt")
    R<String> generateSalt(@NotNull @RequestParam(value = "username") String username);

    /**
     * 生成用户 Token 令牌
     *
     * @param user
     * @return R<String>
     */
    @PostMapping("/generate")
    R<String> generateToken(@Validated(Auth.class) @RequestBody User user);

    /**
     * 检测用户 Token 令牌是否有效
     *
     * @param username Username
     * @param token    Token
     * @return R<Boolean>
     */
    @GetMapping("/check")
    R<Boolean> checkTokenValid(@NotNull @RequestParam(value = "username") String username, @NotNull @RequestParam(value = "token") String token);

    /**
     * 注销用户的Token令牌
     *
     * @param username Username
     * @return R<Boolean>
     */
    @GetMapping("/cancel")
    R<Boolean> cancelToken(@NotNull @RequestParam(value = "username") String username);

}
