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

import com.pnoker.api.center.auth.hystrix.AuthFeignApiHystrix;
import com.pnoker.common.base.bean.Response;
import com.pnoker.common.base.constant.Common;
import com.pnoker.common.base.dto.auth.TokenDto;
import com.pnoker.common.base.entity.auth.User;
import org.springframework.cloud.openfeign.FeignClient;
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
     * 检测用户名是否存在
     *
     * @param username
     * @return true/false
     */
    @GetMapping("/check/exist/{username}")
    Response<Boolean> checkExist(@PathVariable(value = "username") String username);

    /**
     * 检测用Token是否有效
     *
     * @param token
     * @return true/false
     */
    @GetMapping("/check/token/{token}")
    Response<Boolean> checkToken(@PathVariable(value = "token") String token);

    /**
     * 获取Token
     *
     * @param user
     * @return true/false
     */
    @PostMapping("/token")
    Response<TokenDto> token(@RequestBody User user);

}
