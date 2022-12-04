/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.pnoker.center.auth.bean.TokenValid;
import io.github.pnoker.center.auth.bean.UserLimit;
import io.github.pnoker.center.auth.service.TenantBindService;
import io.github.pnoker.center.auth.service.TenantService;
import io.github.pnoker.center.auth.service.TokenService;
import io.github.pnoker.center.auth.service.UserService;
import io.github.pnoker.common.constant.cache.TimeoutConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SuffixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Tenant;
import io.github.pnoker.common.model.User;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.KeyUtil;
import io.github.pnoker.common.utils.RedisUtil;
import io.github.pnoker.common.utils.TimeUtil;
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
 * @since 2022.1.0
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
        String redisSaltKey = PrefixConstant.USER + SuffixConstant.SALT + SymbolConstant.SEPARATOR + username + SymbolConstant.HASHTAG + tenant.getId();
        String salt = redisUtil.getKey(redisSaltKey, String.class);
        if (CharSequenceUtil.isBlank(salt)) {
            salt = RandomUtil.randomString(16);
            redisUtil.setKey(redisSaltKey, salt, TimeoutConstant.SALT_CACHE_TIMEOUT, TimeUnit.MINUTES);
        }
        return salt;
    }

    @Override
    public String generateToken(String username, String salt, String password, String tenantName) {
        // todo 此处一个bug，会抛异常，导致无法记录失败登录次数
        Tenant tenant = tenantService.selectByName(tenantName);
        checkUserLimit(username, tenant.getId());
        User user = userService.selectByName(username, false);
        if (tenant.getEnable() && user.getEnable()) {
            tenantBindService.selectByTenantIdAndUserId(tenant.getId(), user.getId());
            String redisSaltKey = PrefixConstant.USER + SuffixConstant.SALT + SymbolConstant.SEPARATOR + username + SymbolConstant.HASHTAG + tenant.getId();
            String saltValue = redisUtil.getKey(redisSaltKey, String.class);
            if (CharSequenceUtil.isNotEmpty(saltValue)
                    && saltValue.equals(salt)
                    && DecodeUtil.md5(user.getPassword() + saltValue).equals(password)) {
                String redisTokenKey = PrefixConstant.USER + SuffixConstant.TOKEN + SymbolConstant.SEPARATOR + username + SymbolConstant.HASHTAG + tenant.getId();
                String token = KeyUtil.generateToken(username, saltValue, tenant.getId());
                redisUtil.setKey(redisTokenKey, token, TimeoutConstant.TOKEN_CACHE_TIMEOUT, TimeUnit.HOURS);
                return token;
            }
        }
        updateUserLimit(username, tenant.getId(), true);
        throw new ServiceException("Invalid username、password、tenant");
    }

    @Override
    public TokenValid checkTokenValid(String username, String salt, String token, String tenantName) {
        // todo 此处一个bug，会抛异常，导致无法记录失败登录次数
        Tenant tenant = tenantService.selectByName(tenantName);
        String redisKey = PrefixConstant.USER + SuffixConstant.TOKEN + SymbolConstant.SEPARATOR + username + SymbolConstant.HASHTAG + tenant.getId();
        String redisToken = redisUtil.getKey(redisKey, String.class);
        if (CharSequenceUtil.isBlank(redisToken) || !redisToken.equals(token)) {
            return new TokenValid(false, null);
        }
        try {
            Claims claims = KeyUtil.parserToken(username, salt, token, tenant.getId());
            return new TokenValid(true, claims.getExpiration());
        } catch (Exception e) {
            return new TokenValid(false, null);
        }
    }

    @Override
    public Boolean cancelToken(String username, String tenantName) {
        Tenant tenant = tenantService.selectByName(tenantName);
        String redisKey = PrefixConstant.USER + SuffixConstant.TOKEN + SymbolConstant.SEPARATOR + username + SymbolConstant.HASHTAG + tenant.getId();
        redisUtil.deleteKey(redisKey);
        return true;
    }

    /**
     * 检测用户登录限制，返回该用户是否受限
     *
     * @param username 用户名称
     * @param tenantId 租户ID
     */
    private void checkUserLimit(String username, String tenantId) {
        String redisKey = PrefixConstant.USER + SuffixConstant.LIMIT + SymbolConstant.SEPARATOR + username + SymbolConstant.HASHTAG + tenantId;
        UserLimit limit = redisUtil.getKey(redisKey, UserLimit.class);
        if (ObjectUtil.isNotNull(limit) && limit.getTimes() >= 5) {
            Date now = new Date();
            long interval = limit.getExpireTime().getTime() - now.getTime();
            if (interval > 0) {
                limit = updateUserLimit(username, tenantId, false);
                throw new ServiceException("Access restricted，Please try again after {}", TimeUtil.completeFormat(limit.getExpireTime()));
            }
        }
    }

    /**
     * 更新用户登录限制
     *
     * @param username   用户名称
     * @param tenantId   租户ID
     * @param expireTime Expire Time
     * @return UserLimit
     */
    private UserLimit updateUserLimit(String username, String tenantId, boolean expireTime) {
        int amount = TimeoutConstant.USER_LIMIT_TIMEOUT;
        String redisKey = PrefixConstant.USER + SuffixConstant.LIMIT + SymbolConstant.SEPARATOR + username + SymbolConstant.HASHTAG + tenantId;
        UserLimit limit = Optional.ofNullable(redisUtil.getKey(redisKey, UserLimit.class)).orElse(new UserLimit(0, new Date()));
        limit.setTimes(limit.getTimes() + 1);
        if (limit.getTimes() > 20) {
            //TODO 拉黑IP和锁定用户操作，然后通过Gateway进行拦截
            amount = 24 * 60;
        } else if (limit.getTimes() > 5) {
            amount = limit.getTimes() * TimeoutConstant.USER_LIMIT_TIMEOUT;
        }
        if (expireTime) {
            limit.setExpireTime(TimeUtil.expireTime(amount, Calendar.MINUTE));
        }
        redisUtil.setKey(redisKey, limit, 1, TimeUnit.DAYS);
        return limit;
    }
}