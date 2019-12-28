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

package com.pnoker.auth.service.impl;

import com.pnoker.auth.service.TokenService;
import com.pnoker.auth.service.UserService;
import com.pnoker.common.model.auth.User;
import com.pnoker.common.utils.KeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 令牌服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {
    @Resource
    private UserService userService;

    @Override
    public String generateToken(User user) {
        User select = userService.selectByName(user.getName());
        if (null != select) {
            if (select.getName().equals(user.getName())) {
                return KeyUtil.generateToken(user.getName());
            }
        }
        return null;
    }

    @Override
    public boolean checkTokenValid(String token) {
        try {
            KeyUtil.parserToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
