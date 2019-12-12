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

package com.pnoker.center.auth.service.impl;

import com.pnoker.center.auth.service.AuthService;
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.entity.auth.User;
import com.pnoker.dbs.api.user.feign.UserDbsFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>Auth 接口实现
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    @Resource
    private UserDbsFeignClient userDbsFeignClient;

    @Override
    public TokenDto generateToken(User user) {
        return null;
    }
}
