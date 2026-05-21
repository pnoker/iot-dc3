/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.auth.biz.impl;

import io.github.pnoker.common.auth.biz.TokenService;
import io.github.pnoker.common.auth.cache.TokenDenylistCache;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.bo.TenantBindBO;
import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.auth.entity.bo.UserPasswordBO;
import io.github.pnoker.common.auth.service.TenantBindService;
import io.github.pnoker.common.auth.service.TenantService;
import io.github.pnoker.common.auth.service.UserLoginService;
import io.github.pnoker.common.auth.service.UserPasswordService;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.KeyUtil;
import io.github.pnoker.common.utils.PasswordUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Token validation and management service implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final TenantService tenantService;

    private final UserLoginService userLoginService;

    private final UserPasswordService userPasswordService;

    private final TenantBindService tenantBindService;

    private final TokenDenylistCache tokenDenylistCache;

    @Override
    public String generateSalt(String loginName, String tenantCode) {
        TenantBO tenantBO = tenantService.getByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        return UUID.randomUUID().toString();
    }

    @Override
    public String generateToken(String loginName, String salt, String password, String tenantCode) {
        TenantBO tenantBO = tenantService.getByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        UserLoginBO userLogin = userLoginService.getByLoginName(loginName, false);
        if (Objects.isNull(userLogin)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        TenantBindBO tenantBindBO = tenantBindService.getByTenantIdAndUserId(tenantBO.getId(),
                userLogin.getUserId());
        if (Objects.isNull(tenantBindBO)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        UserPasswordBO userPasswordBO = userPasswordService.getById(userLogin.getUserPasswordId());
        if (Objects.isNull(userPasswordBO)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        if (StringUtils.isEmpty(salt)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }

        String storedHash = userPasswordBO.getLoginPassword();
        if (!PasswordUtil.verify(password, storedHash)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        return KeyUtil.generateToken(loginName, salt, tenantBO.getId());
    }

    @Override
    public boolean tryCancelToken(String loginName, String tenantCode) {
        TenantBO tenantBO = tenantService.getByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            return false;
        }
        UserLoginBO userLogin = userLoginService.getByLoginName(loginName, false);
        if (Objects.isNull(userLogin)) {
            return false;
        }
        long logoutEpochMs = System.currentTimeMillis();
        tokenDenylistCache.markLogout(loginName, tenantCode, logoutEpochMs);
        log.info("User logout, loginName={}, tenantCode={}, logoutEpochMs={}", loginName, tenantCode, logoutEpochMs);
        return true;
    }

    @Override
    public TokenValid checkValid(String loginName, String salt, String token, String tenantCode) {
        TenantBO tenantBO = tenantService.getByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }

        TokenValid tokenValid = new TokenValid(false, null);
        if (StringUtils.isBlank(token)) {
            return tokenValid;
        }

        UserLoginBO userLogin = userLoginService.getByLoginName(loginName, false);
        if (Objects.isNull(userLogin) || Objects.isNull(tenantBindService.getByTenantIdAndUserId(tenantBO.getId(),
                userLogin.getUserId()))) {
            return tokenValid;
        }

        try {
            Claims claims = KeyUtil.parserToken(loginName, salt, token, tenantBO.getId());
            Date issuedAt = claims.getIssuedAt();
            long issuedAtEpochMs = Objects.nonNull(issuedAt) ? issuedAt.getTime() : 0L;
            if (tokenDenylistCache.isRevoked(loginName, tenantCode, issuedAtEpochMs)) {
                tokenValid.setExpireTime(claims.getExpiration());
                return tokenValid;
            }
            tokenValid.setValid(true);
            tokenValid.setExpireTime(claims.getExpiration());
            return tokenValid;
        } catch (Exception e) {
            return tokenValid;
        }
    }

}
