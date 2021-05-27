/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
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

import com.dc3.api.center.auth.hystrix.TokenClientHystrix;
import com.dc3.common.bean.Login;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.utils.Dc3Util;
import com.dc3.common.valid.Auth;
import com.dc3.common.valid.Check;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
     * @param name Username
     * @return R<String>
     */
    @PostMapping("/salt")
    R<String> generateSalt(@NotNull @RequestBody String name);

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
    R<Long> checkTokenValid(@Validated(Check.class) @RequestBody Login login);

    /**
     * 注销用户的Token令牌
     *
     * @param name Username
     * @return R<Boolean>
     */
    @PostMapping("/cancel")
    R<Boolean> cancelToken(@NotNull @RequestBody String name);

    static void main(String[] args) {
        System.out.println(Dc3Util.md5("10e339be1130a90dc1b9ff0332abced6" + "dsb785i4ikx0h4wt"));
    }
}
