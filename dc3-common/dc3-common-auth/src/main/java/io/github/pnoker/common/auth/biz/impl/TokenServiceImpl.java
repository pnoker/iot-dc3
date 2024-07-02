/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.common.auth.biz.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.pnoker.common.auth.biz.TokenService;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.auth.entity.bean.UserLimit;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.bo.TenantBindBO;
import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.auth.entity.bo.UserPasswordBO;
import io.github.pnoker.common.auth.service.TenantBindService;
import io.github.pnoker.common.auth.service.TenantService;
import io.github.pnoker.common.auth.service.UserLoginService;
import io.github.pnoker.common.auth.service.UserPasswordService;
import io.github.pnoker.common.constant.cache.TimeoutConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SuffixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.redis.service.RedisService;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.KeyUtil;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
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
    private UserLoginService userLoginService;
    @Resource
    private UserPasswordService userPasswordService;
    @Resource
    private TenantBindService tenantBindService;

    @Resource
    private RedisService redisService;

    @Override
    public String generateSalt(String loginName, String tenantCode) {
        checkUserLimit(loginName, tenantCode);
        TenantBO tenantBO = tenantService.selectByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            throw new NotFoundException("租户, 用户信息不匹配");
        }
        String redisSaltKey = PrefixConstant.USER + SuffixConstant.SALT + SymbolConstant.COLON + loginName + SymbolConstant.HASHTAG + tenantBO.getId();
        String salt = redisService.getKey(redisSaltKey);
        if (CharSequenceUtil.isBlank(salt)) {
            salt = RandomUtil.randomString(16);
            redisService.setKey(redisSaltKey, salt, TimeoutConstant.SALT_CACHE_TIMEOUT, TimeUnit.MINUTES);
        }
        return salt;
    }

    @Override
    public String generateToken(String loginName, String salt, String password, String tenantCode) {
        checkUserLimit(loginName, tenantCode);
        TenantBO tenantBO = tenantService.selectByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            updateUserLimit(loginName, tenantCode);
            throw new NotFoundException("租户, 用户信息不匹配");
        }
        UserLoginBO userLogin = userLoginService.selectByLoginName(loginName, false);
        if (Objects.isNull(userLogin)) {
            updateUserLimit(loginName, tenantCode);
            throw new NotFoundException("租户, 用户信息不匹配");
        }
        TenantBindBO tenantBindBO = tenantBindService.selectByTenantIdAndUserId(tenantBO.getId(), userLogin.getUserId());
        if (Objects.isNull(tenantBindBO)) {
            updateUserLimit(loginName, tenantCode);
            throw new NotFoundException("租户, 用户信息不匹配");
        }
        UserPasswordBO userPasswordBO = userPasswordService.selectById(userLogin.getUserPasswordId());
        if (Objects.isNull(userPasswordBO)) {
            updateUserLimit(loginName, tenantCode);
            throw new NotFoundException("租户, 用户信息不匹配");
        }
        String redisSaltKey = PrefixConstant.USER + SuffixConstant.SALT + SymbolConstant.COLON + loginName + SymbolConstant.HASHTAG + tenantBO.getId();
        String redisSaltValue = redisService.getKey(redisSaltKey);
        if (CharSequenceUtil.isEmpty(redisSaltValue)) {
            updateUserLimit(loginName, tenantCode);
            throw new NotFoundException("租户, 用户信息不匹配");
        }
        String md5Password = DecodeUtil.md5(userPasswordBO.getLoginPassword(), redisSaltValue);
        if (!redisSaltValue.equals(salt) || !md5Password.equals(password)) {
            updateUserLimit(loginName, tenantCode);
            throw new ServiceException("租户, 用户信息不匹配");
        }
        String redisTokenKey = PrefixConstant.USER + SuffixConstant.TOKEN + SymbolConstant.COLON + loginName + SymbolConstant.HASHTAG + tenantBO.getId();
        String token = KeyUtil.generateToken(loginName, redisSaltValue, tenantBO.getId());
        redisService.setKey(redisTokenKey, token, TimeoutConstant.TOKEN_CACHE_TIMEOUT, TimeUnit.HOURS);
        return token;
    }

    @Override
    public TokenValid checkValid(String loginName, String salt, String token, String tenantCode) {
        TenantBO tenantBO = tenantService.selectByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            throw new NotFoundException("租户, 用户信息不匹配");
        }
        String redisKey = PrefixConstant.USER + SuffixConstant.TOKEN + SymbolConstant.COLON + loginName + SymbolConstant.HASHTAG + tenantBO.getId();
        String redisToken = redisService.getKey(redisKey);
        if (CharSequenceUtil.isBlank(redisToken) || !redisToken.equals(token)) {
            return new TokenValid(false, null);
        }
        try {
            Claims claims = KeyUtil.parserToken(loginName, salt, token, tenantBO.getId());
            return new TokenValid(true, claims.getExpiration());
        } catch (Exception e) {
            return new TokenValid(false, null);
        }
    }

    @Override
    public Boolean cancelToken(String loginName, String tenantCode) {
        TenantBO tenantBO = tenantService.selectByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            throw new NotFoundException("租户, 用户信息不匹配");
        }
        String redisKey = PrefixConstant.USER + SuffixConstant.TOKEN + SymbolConstant.COLON + loginName + SymbolConstant.HASHTAG + tenantBO.getId();
        redisService.deleteKey(redisKey);
        return true;
    }

    /**
     * 检测用户登录限制, 返回该用户是否受限
     *
     * @param loginName  登录名称
     * @param tenantCode 租户编号
     */
    private void checkUserLimit(String loginName, String tenantCode) {
        String redisKey = PrefixConstant.USER + SuffixConstant.LIMIT + SymbolConstant.COLON + loginName + SymbolConstant.HASHTAG + tenantCode;
        Object key = redisService.getKey(redisKey);
        UserLimit limit = redisService.getKey(redisKey);
        if (Objects.isNull(limit) || limit.getTimes() < 5) {
            return;
        }
        boolean isAfter = limit.getExpireTime().isAfter(LocalDateTime.now());
        if (isAfter) {
            throw new ServiceException("访问受限, 请在 {} 之后再重试", LocalDateTimeUtil.completeFormat(limit.getExpireTime()));
        }
    }

    /**
     * 更新用户登录限制
     *
     * @param loginName  登录名称
     * @param tenantCode 租户编号
     * @return UserLimit
     */
    private UserLimit updateUserLimit(String loginName, String tenantCode) {
        String redisKey = PrefixConstant.USER + SuffixConstant.LIMIT + SymbolConstant.COLON + loginName + SymbolConstant.HASHTAG + tenantCode;
        UserLimit userLimit = redisService.getKey(redisKey);
        UserLimit limit = Optional.ofNullable(userLimit).orElse(new UserLimit(0, LocalDateTime.now()));
        limit.setTimes(limit.getTimes() + 1);
        limit.setExpireTime(LocalDateTimeUtil.expireTime(limit.getTimes() * TimeoutConstant.USER_LIMIT_TIMEOUT, ChronoUnit.MINUTES));
        redisService.setKey(redisKey, limit, 7, TimeUnit.DAYS);
        return limit;
    }
}
