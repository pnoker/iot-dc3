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

package com.pnoker.center.auth.api;

import com.pnoker.api.center.auth.feign.AuthFeignClient;
import com.pnoker.common.base.bean.Response;
import com.pnoker.common.base.constant.Common;
import com.pnoker.common.base.dto.auth.TokenDto;
import com.pnoker.common.base.entity.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>auth rest api
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_AUTH_URL_PREFIX)
public class AuthApi implements AuthFeignClient {

    @Override
    public Response<Boolean> checkExist(String username) {
        return null;
    }

    @Override
    public Response<Boolean> checkToken(String token) {
        return null;
    }

    @Override
    public Response<TokenDto> token(User user) {
        return null;
    }
}
