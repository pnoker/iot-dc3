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

package com.dc3.center.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.dc3.center.auth.bean.TokenValid;
import com.dc3.center.auth.bean.UserLimit;
import com.dc3.center.auth.service.TokenService;
import com.dc3.center.auth.service.UserService;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.User;
import com.dc3.common.utils.Dc3Util;
import com.dc3.common.utils.KeyUtil;
import com.dc3.common.utils.RedisUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    @Resource
    private RedisUtil redisUtil;

    @Override
    public String generateSalt(String username) {
        String redisSaltKey = Common.Cache.USER + Common.Cache.SALT + Common.Cache.SEPARATOR + username;
        String salt = redisUtil.getKey(redisSaltKey, String.class);
        if (StrUtil.isBlank(salt)) {
            salt = RandomUtil.randomString(16);
            redisUtil.setKey(redisSaltKey, salt, Common.Cache.SALT_CACHE_TIMEOUT, TimeUnit.MINUTES);
        }
        return salt;
    }

    @Override
    public String generateToken(User user) {
        checkUserLimit(user.getName());
        User select = userService.selectByName(user.getName());
        if (null != select) {
            String redisSaltKey = Common.Cache.USER + Common.Cache.SALT + Common.Cache.SEPARATOR + user.getName();
            String salt = redisUtil.getKey(redisSaltKey, String.class);
            if (StrUtil.isNotBlank(salt)) {
                if (Dc3Util.md5(select.getPassword() + salt).equals(user.getPassword())) {
                    String redisTokenKey = Common.Cache.USER + Common.Cache.TOKEN + Common.Cache.SEPARATOR + user.getName();
                    String token = KeyUtil.generateToken(user.getName(), salt);
                    redisUtil.setKey(redisTokenKey, token, Common.Cache.TOKEN_CACHE_TIMEOUT, TimeUnit.HOURS);
                    return token;
                }
            }
        }
        updateUserLimit(user.getName(), true);
        throw new ServiceException("Invalid username、password、salt");
    }

    @Override
    public TokenValid checkTokenValid(String username, String salt, String token) {
        String redisToken = redisUtil.getKey(Common.Cache.USER + Common.Cache.TOKEN + Common.Cache.SEPARATOR + username, String.class);
        if (StrUtil.isBlank(redisToken) || !redisToken.equals(token)) {
            return new TokenValid(false, null);
        }
        try {
            Claims claims = KeyUtil.parserToken(username, salt, token);
            return new TokenValid(true, claims.getExpiration());
        } catch (Exception e) {
            return new TokenValid(false, null);
        }
    }

    @Override
    public boolean cancelToken(String username) {
        redisUtil.removeKey(Common.Cache.USER + Common.Cache.TOKEN + Common.Cache.SEPARATOR + username);
        return true;
    }

    /**
     * 检测用户登录限制，返回该用户是否受限
     *
     * @param username Username
     */
    private void checkUserLimit(String username) {
        String redisKey = Common.Cache.USER + Common.Cache.LIMIT + Common.Cache.SEPARATOR + username;
        UserLimit limit = redisUtil.getKey(redisKey, UserLimit.class);
        if (null != limit && limit.getTimes() >= 5) {
            Date now = new Date();
            long interval = limit.getExpireTime().getTime() - now.getTime();
            if (interval > 0) {
                limit = updateUserLimit(username, false);
                throw new ServiceException("Access restricted，Please try again after {}", Dc3Util.formatData(limit.getExpireTime()));
            }
        }
    }

    /**
     * 更新用户登录限制
     *
     * @param username Username
     * @return UserLimit
     */
    private UserLimit updateUserLimit(String username, boolean expireTime) {
        int amount = Common.Cache.USER_LIMIT_TIMEOUT;
        String redisKey = Common.Cache.USER + Common.Cache.LIMIT + Common.Cache.SEPARATOR + username;
        UserLimit limit = (UserLimit) Optional.ofNullable(redisUtil.getKey(redisKey, UserLimit.class)).orElse(new UserLimit(0, new Date()));
        limit.setTimes(limit.getTimes() + 1);
        if (limit.getTimes() > 20) {
            //TODO 拉黑IP和锁定用户操作，然后通过Gateway进行拦截
            amount = 24 * 60;
        } else if (limit.getTimes() > 5) {
            amount = limit.getTimes() * Common.Cache.USER_LIMIT_TIMEOUT;
        }
        if (expireTime) {
            limit.setExpireTime(Dc3Util.expireTime(amount, Calendar.MINUTE));
        }
        redisUtil.setKey(redisKey, limit, 1, TimeUnit.DAYS);
        return limit;
    }
}
