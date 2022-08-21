/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import io.github.pnoker.center.auth.bean.TokenValid;
import io.github.pnoker.center.auth.bean.UserLimit;
import io.github.pnoker.center.auth.service.TenantBindService;
import io.github.pnoker.center.auth.service.TenantService;
import io.github.pnoker.center.auth.service.TokenService;
import io.github.pnoker.center.auth.service.UserService;
import io.github.pnoker.common.constant.CacheConstant;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Tenant;
import io.github.pnoker.common.model.User;
import io.github.pnoker.common.utils.Dc3Util;
import io.github.pnoker.common.utils.KeyUtil;
import io.github.pnoker.common.utils.RedisUtil;
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
    private TenantService tenantService;
    @Resource
    private UserService userService;
    @Resource
    private TenantBindService tenantBindService;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public String generateSalt(String username, String tenantName) {
        // todo 此处一个bug，会抛异常，导致无法记录失败登录次数
        Tenant tenant = tenantService.selectByName(tenantName);
        String redisSaltKey = CacheConstant.Entity.USER + CacheConstant.Suffix.SALT + CommonConstant.Symbol.SEPARATOR + username;
        String salt = redisUtil.getKey(redisSaltKey, String.class);
        if (StrUtil.isBlank(salt)) {
            salt = RandomUtil.randomString(16);
            redisUtil.setKey(redisSaltKey, salt, CacheConstant.Timeout.SALT_CACHE_TIMEOUT, TimeUnit.MINUTES);
        }
        return salt;
    }

    @Override
    public String generateToken(String username, String salt, String password, String tenantName) {
        checkUserLimit(username);
        // todo 此处一个bug，会抛异常，导致无法记录失败登录次数
        Tenant tenant = tenantService.selectByName(tenantName);
        User user = userService.selectByName(username, false);
        if (tenant.getEnable() && user.getEnable()) {
            tenantBindService.selectByTenantIdAndUserId(tenant.getId(), user.getId());
            String redisSaltKey = CacheConstant.Entity.USER + CacheConstant.Suffix.SALT + CommonConstant.Symbol.SEPARATOR + username;
            String saltValue = redisUtil.getKey(redisSaltKey, String.class);
            if (StrUtil.isNotEmpty(saltValue) && saltValue.equals(salt)) {
                if (Dc3Util.md5(user.getPassword() + saltValue).equals(password)) {
                    String redisTokenKey = CacheConstant.Entity.USER + CacheConstant.Suffix.TOKEN + CommonConstant.Symbol.SEPARATOR + username;
                    String token = KeyUtil.generateToken(username, saltValue, tenant.getId());
                    redisUtil.setKey(redisTokenKey, token, CacheConstant.Timeout.TOKEN_CACHE_TIMEOUT, TimeUnit.HOURS);
                    return token;
                }
            }
        }
        updateUserLimit(username, true);
        throw new ServiceException("Invalid username、password、tenant");
    }

    @Override
    public TokenValid checkTokenValid(String username, String salt, String token, String tenantName) {
        // todo 此处一个bug，会抛异常，导致无法记录失败登录次数
        Tenant tenant = tenantService.selectByName(tenantName);
        String redisToken = redisUtil.getKey(CacheConstant.Entity.USER + CacheConstant.Suffix.TOKEN + CommonConstant.Symbol.SEPARATOR + username, String.class);
        if (StrUtil.isBlank(redisToken) || !redisToken.equals(token)) {
            return new TokenValid(false, null);
        }
        try {
            // todo 需要传 tenantId
            Claims claims = KeyUtil.parserToken(username, salt, token, tenant.getId());
            return new TokenValid(true, claims.getExpiration());
        } catch (Exception e) {
            return new TokenValid(false, null);
        }
    }

    @Override
    public boolean cancelToken(String username, String tenantName) {
        Tenant tenant = tenantService.selectByName(tenantName);
        redisUtil.deleteKey(CacheConstant.Entity.USER + CacheConstant.Suffix.TOKEN + CommonConstant.Symbol.SEPARATOR + username);
        return true;
    }

    /**
     * 检测用户登录限制，返回该用户是否受限
     *
     * @param username Username
     */
    private void checkUserLimit(String username) {
        String redisKey = CacheConstant.Entity.USER + CacheConstant.Suffix.LIMIT + CommonConstant.Symbol.SEPARATOR + username;
        UserLimit limit = redisUtil.getKey(redisKey, UserLimit.class);
        if (ObjectUtil.isNotNull(limit) && limit.getTimes() >= 5) {
            Date now = new Date();
            long interval = limit.getExpireTime().getTime() - now.getTime();
            if (interval > 0) {
                limit = updateUserLimit(username, false);
                throw new ServiceException("Access restricted，Please try again after {}", Dc3Util.formatCompleteData(limit.getExpireTime()));
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
        int amount = CacheConstant.Timeout.USER_LIMIT_TIMEOUT;
        String redisKey = CacheConstant.Entity.USER + CacheConstant.Suffix.LIMIT + CommonConstant.Symbol.SEPARATOR + username;
        UserLimit limit = Optional.ofNullable(redisUtil.getKey(redisKey, UserLimit.class)).orElse(new UserLimit(0, new Date()));
        limit.setTimes(limit.getTimes() + 1);
        if (limit.getTimes() > 20) {
            //TODO 拉黑IP和锁定用户操作，然后通过Gateway进行拦截
            amount = 24 * 60;
        } else if (limit.getTimes() > 5) {
            amount = limit.getTimes() * CacheConstant.Timeout.USER_LIMIT_TIMEOUT;
        }
        if (expireTime) {
            limit.setExpireTime(Dc3Util.expireTime(amount, Calendar.MINUTE));
        }
        redisUtil.setKey(redisKey, limit, 1, TimeUnit.DAYS);
        return limit;
    }
}
