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

package com.dc3.center.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.dc3.center.auth.service.TokenService;
import com.dc3.center.auth.service.UserService;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.User;
import com.dc3.common.utils.KeyUtil;
import com.dc3.common.utils.RedisUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 令牌服务接口实现类
 *
 * @author pnoker
 */
@Service
public class TokenServiceImpl implements TokenService {

    @Resource
    private UserService userService;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public String randomSalt(String username) {
        String redisSaltKey = Common.Cache.USER + Common.Cache.SALT + "::" + username;
        String salt = redisUtil.getKey(redisSaltKey);
        if (StringUtils.isBlank(salt)) {
            salt = RandomUtil.randomString(8);
            redisUtil.setKey(redisSaltKey, salt, Common.Cache.TOKEN_CACHE_TIMEOUT, TimeUnit.MINUTES);
        }
        return salt;
    }

    @Override
    public String generateToken(User user) {
        User select = userService.selectByName(user.getName());
        if (null != select) {
            if (select.getPassword().equals(user.getPassword())) {
                String token = KeyUtil.generateToken(user.getName());
                redisUtil.setKey(Common.Cache.USER + Common.Cache.TOKEN + "::" + user.getName(), token, Common.Cache.TOKEN_CACHE_TIMEOUT, TimeUnit.HOURS);
                return token;
            }
        }
        throw new ServiceException("用户名和密码不匹配");
    }

    @Override
    public boolean checkTokenValid(String username, String token) {
        String redisToken = redisUtil.getKey(Common.Cache.USER + Common.Cache.TOKEN + "::" + username);
        if (StringUtils.isBlank(redisToken) || !redisToken.equals(token)) {
            return false;
        }
        try {
            KeyUtil.parserToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean cancelToken(String username) {
        redisUtil.removeKey(Common.Cache.USER + Common.Cache.TOKEN + "::" + username);
        return true;
    }
}
