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
import io.github.pnoker.common.auth.dal.PrincipalManager;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.common.auth.service.LocalCredentialService;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.auth.service.TenantService;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.enums.RequirePasswordChangeFlagEnum;
import io.github.pnoker.common.exception.PasswordChangeRequiredException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.KeyUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    private final LocalCredentialService localCredentialService;

    private final TenantMembershipService tenantMembershipService;

    private final PrincipalManager principalManager;

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
        LocalCredentialBO credential = localCredentialService.getByLoginName(loginName, false);
        if (Objects.isNull(credential)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        if (!tenantMembershipService.isTenantMember(tenantBO.getId(), credential.getPrincipalId())) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        if (StringUtils.isEmpty(salt)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }

        if (!localCredentialService.verifyPassword(credential, password)) {
            localCredentialService.recordFailedLogin(credential.getId());
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        localCredentialService.recordSuccessfulLogin(credential.getId());

        if (Objects.nonNull(credential.getPasswordExpireTime())
                && credential.getPasswordExpireTime().isBefore(LocalDateTime.now())) {
            throw new PasswordChangeRequiredException(ErrorCode.PASSWORD_EXPIRED);
        }
        if (RequirePasswordChangeFlagEnum.REQUIRED == credential.getRequirePasswordChange()) {
            throw new PasswordChangeRequiredException(ErrorCode.PASSWORD_CHANGE_REQUIRED);
        }

        markPrincipalLogin(credential.getPrincipalId());
        return KeyUtil.generateToken(String.valueOf(credential.getPrincipalId()), salt, tenantBO.getId());
    }

    @Override
    public void changePassword(String loginName, String currentPassword, String newPassword, String tenantCode) {
        TenantBO tenantBO = tenantService.getByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        LocalCredentialBO credential = localCredentialService.getByLoginName(loginName, false);
        if (Objects.isNull(credential)
                || !tenantMembershipService.isTenantMember(tenantBO.getId(), credential.getPrincipalId())) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        localCredentialService.changePassword(loginName, currentPassword, newPassword);
    }

    @Override
    public boolean tryCancelToken(String loginName, String tenantCode) {
        TenantBO tenantBO = tenantService.getByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            return false;
        }
        LocalCredentialBO credential = localCredentialService.getByLoginName(loginName, false);
        if (Objects.isNull(credential) || !tenantMembershipService.isTenantMember(tenantBO.getId(),
                credential.getPrincipalId())) {
            return false;
        }
        long logoutEpochMs = System.currentTimeMillis();
        String principalKey = String.valueOf(credential.getPrincipalId());
        tokenDenylistCache.markLogout(principalKey, tenantCode, logoutEpochMs);
        log.info("Principal logout, principalId={}, tenantCode={}, logoutEpochMs={}", principalKey, tenantCode,
                logoutEpochMs);
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

        LocalCredentialBO credential = localCredentialService.getByLoginName(loginName, false);
        if (Objects.isNull(credential) || !tenantMembershipService.isTenantMember(tenantBO.getId(),
                credential.getPrincipalId())) {
            return tokenValid;
        }

        try {
            String principalKey = String.valueOf(credential.getPrincipalId());
            Claims claims = KeyUtil.parserToken(principalKey, salt, token, tenantBO.getId());
            Date issuedAt = claims.getIssuedAt();
            long issuedAtEpochMs = Objects.nonNull(issuedAt) ? issuedAt.getTime() : 0L;
            if (tokenDenylistCache.isRevoked(principalKey, tenantCode, issuedAtEpochMs)) {
                tokenValid.setExpireTime(claims.getExpiration());
                return tokenValid;
            }
            tokenValid.setValid(true);
            tokenValid.setExpireTime(claims.getExpiration());
            return tokenValid;
        } catch (Exception e) {
            log.warn("Token validation failed", e);
            return tokenValid;
        }
    }

    /**
     * Stamp the last-login time on a principal after a successful login.
     *
     * @param principalId the principal that logged in
     */
    private void markPrincipalLogin(Long principalId) {
        PrincipalDO principal = principalManager.getById(principalId);
        if (Objects.isNull(principal)) {
            return;
        }
        principal.setLastLoginTime(LocalDateTime.now());
        principal.setOperateTime(null);
        principalManager.updateById(principal);
    }

}
