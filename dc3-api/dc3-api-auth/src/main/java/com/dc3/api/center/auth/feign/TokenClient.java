/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.api.center.auth.feign;

import com.dc3.api.center.auth.fallback.TokenClientFallback;
import com.dc3.common.bean.Login;
import com.dc3.common.bean.R;
import com.dc3.common.constant.ServiceConstant;
import com.dc3.common.valid.Auth;
import com.dc3.common.valid.Check;
import com.dc3.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 令牌 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = ServiceConstant.Auth.TOKEN_URL_PREFIX, name = ServiceConstant.Auth.SERVICE_NAME, fallbackFactory = TokenClientFallback.class)
public interface TokenClient {

    /**
     * 生成用户随机 Salt
     *
     * @param login Login
     * @return R<String>
     */
    @PostMapping("/salt")
    R<String> generateSalt(@Validated(Update.class) @RequestBody Login login);

    /**
     * 生成用户 Token 令牌
     *
     * @param login Login
     * @return R<String>
     */
    @PostMapping("/generate")
    R<String> generateToken(@Validated(Auth.class) @RequestBody Login login);

    /**
     * 检测用户 Token 令牌是否有效
     *
     * @param login Login
     * @return R<Boolean>
     */
    @PostMapping("/check")
    R<String> checkTokenValid(@Validated(Check.class) @RequestBody Login login);

    /**
     * 注销用户的Token令牌
     *
     * @param login Login
     * @return R<Boolean>
     */
    @PostMapping("/cancel")
    R<Boolean> cancelToken(@Validated(Update.class) @RequestBody Login login);
}
