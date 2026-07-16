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

package io.github.pnoker.common.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.LocalCredentialManager;
import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.builder.LocalCredentialBuilder;
import io.github.pnoker.common.auth.entity.model.LocalCredentialDO;
import io.github.pnoker.common.auth.entity.query.LocalCredentialQuery;
import io.github.pnoker.common.auth.service.LocalCredentialService;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.CredentialTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.RequirePasswordChangeFlagEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import io.github.pnoker.common.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Local credential service implementation.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalCredentialServiceImpl implements LocalCredentialService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private static final long LOCK_MINUTES = 15;
    private final LocalCredentialBuilder localCredentialBuilder;
    private final LocalCredentialManager localCredentialManager;
    private final TenantMembershipService tenantMembershipService;
    /**
     * Password validity in days after a change; {@code 0} (default) means passwords never expire.
     */
    @Value("${dc3.auth.password.expire-days:0}")
    private long passwordExpireDays;

    @Override
    public void add(LocalCredentialBO entityBO) {
        prepareForCreate(entityBO);
        checkDuplicate(entityBO, false, true);

        LocalCredentialDO entityDO = localCredentialBuilder.buildDOByBO(entityBO);
        if (!localCredentialManager.save(entityDO)) {
            throw new AddException("Failed to create local credential");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);
        if (!localCredentialManager.removeById(id)) {
            throw new DeleteException("Failed to remove local credential");
        }
    }

    @Override
    public void update(LocalCredentialBO entityBO) {
        LocalCredentialDO current = getDOById(entityBO.getId(), true);
        prepareForUpdate(entityBO, current);
        checkDuplicate(entityBO, true, true);

        LocalCredentialDO entityDO = localCredentialBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!localCredentialManager.updateById(entityDO)) {
            throw new UpdateException("The local credential update failed");
        }
    }

    @Override
    public LocalCredentialBO getById(Long id) {
        return localCredentialBuilder.buildBOByDO(getDOById(id, true));
    }

    @Override
    public Page<LocalCredentialBO> list(LocalCredentialQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<LocalCredentialDO> page = localCredentialManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return localCredentialBuilder.buildBOPageByDOPage(page);
    }

    @Override
    public LocalCredentialBO getByLoginName(String loginName, boolean throwException) {
        if (StringUtils.isBlank(loginName)) {
            if (throwException) {
                throw new EmptyException("The login name is empty");
            }
            return null;
        }
        LambdaQueryWrapper<LocalCredentialDO> wrapper = Wrappers.<LocalCredentialDO>query().lambda();
        wrapper.eq(LocalCredentialDO::getCredentialType, CredentialTypeEnum.PASSWORD.getValue());
        wrapper.eq(LocalCredentialDO::getLoginNameNormalized, normalize(loginName));
        wrapper.eq(LocalCredentialDO::getEnableFlag, EnableFlagEnum.ENABLE.getIndex());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        LocalCredentialDO credential = localCredentialManager.getOne(wrapper);
        if (Objects.isNull(credential)) {
            if (throwException) {
                throw new NotFoundException("Local credential does not exist");
            }
            return null;
        }
        return localCredentialBuilder.buildBOByDO(credential);
    }

    @Override
    public boolean isLoginNameAvailable(String loginName) {
        if (StringUtils.isBlank(loginName)) {
            return false;
        }
        LambdaQueryWrapper<LocalCredentialDO> wrapper = Wrappers.<LocalCredentialDO>query().lambda();
        wrapper.eq(LocalCredentialDO::getCredentialType, CredentialTypeEnum.PASSWORD.getValue());
        wrapper.eq(LocalCredentialDO::getLoginNameNormalized, normalize(loginName));
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        return Objects.isNull(localCredentialManager.getOne(wrapper));
    }

    @Override
    public boolean verifyPassword(LocalCredentialBO credential, String rawPassword) {
        if (Objects.isNull(credential) || StringUtils.isBlank(rawPassword)) {
            return false;
        }
        LocalDateTime lockedUntil = credential.getLockedUntil();
        if (Objects.nonNull(lockedUntil) && lockedUntil.isAfter(LocalDateTime.now())) {
            return false;
        }
        return PasswordUtil.verify(rawPassword, credential.getPasswordHash());
    }

    @Override
    public void resetPassword(Long id, String rawPassword) {
        if (StringUtils.isBlank(rawPassword)) {
            throw new EmptyException("The password is empty");
        }
        LocalCredentialBO credential = getById(id);
        credential.setRawPassword(rawPassword);
        credential.setRequirePasswordChange(RequirePasswordChangeFlagEnum.REQUIRED);
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        update(credential);
    }

    @Override
    public void changePassword(String loginName, String currentPassword, String newPassword) {
        if (StringUtils.isBlank(newPassword)) {
            throw new EmptyException("The new password is empty");
        }
        LocalCredentialBO credential = getByLoginName(loginName, true);
        if (!PasswordUtil.verify(currentPassword, credential.getPasswordHash())) {
            throw new UnAuthorizedException("The current password does not match");
        }
        LocalCredentialDO entityDO = getDOById(credential.getId(), true);
        String hash = PasswordUtil.encode(newPassword);
        LocalDateTime now = LocalDateTime.now();
        entityDO.setPasswordHash(hash);
        entityDO.setPasswordAlgorithm(PasswordUtil.algorithmOfHash(hash).getValue());
        entityDO.setPasswordUpdatedTime(now);
        entityDO.setPasswordExpireTime(passwordExpireDays > 0 ? now.plusDays(passwordExpireDays) : null);
        entityDO.setRequirePasswordChange((byte) 0);
        entityDO.setFailedAttempts(0);
        entityDO.setLockedUntil(null);
        entityDO.setOperateTime(null);
        if (!localCredentialManager.updateById(entityDO)) {
            throw new UpdateException("The password change failed");
        }
    }

    @Override
    public void recordSuccessfulLogin(Long id) {
        LocalCredentialDO credential = getDOById(id, false);
        if (Objects.isNull(credential)) {
            return;
        }
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setOperateTime(null);
        localCredentialManager.updateById(credential);
    }

    @Override
    public void recordFailedLogin(Long id) {
        LocalCredentialDO credential = getDOById(id, false);
        if (Objects.isNull(credential)) {
            return;
        }
        int failedAttempts = Objects.isNull(credential.getFailedAttempts()) ? 1 : credential.getFailedAttempts() + 1;
        credential.setFailedAttempts(failedAttempts);
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            credential.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        }
        credential.setOperateTime(null);
        localCredentialManager.updateById(credential);
    }

    /**
     * Build fuzzy query wrapper for local credential search. Tenant filtering joins
     * through tenant membership since credentials have no tenant_id column.
     *
     * @param entityQuery {@link LocalCredentialQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link LocalCredentialDO}
     */
    private LambdaQueryWrapper<LocalCredentialDO> fuzzyQuery(LocalCredentialQuery entityQuery) {
        LambdaQueryWrapper<LocalCredentialDO> wrapper = Wrappers.<LocalCredentialDO>query().lambda();
        wrapper.eq(Objects.nonNull(entityQuery.getPrincipalId()), LocalCredentialDO::getPrincipalId,
                entityQuery.getPrincipalId());
        wrapper.like(StringUtils.isNotBlank(entityQuery.getLoginName()), LocalCredentialDO::getLoginNameNormalized,
                normalize(entityQuery.getLoginName()));
        wrapper.eq(Objects.nonNull(entityQuery.getCredentialType()), LocalCredentialDO::getCredentialType,
                Objects.isNull(entityQuery.getCredentialType()) ? null : entityQuery.getCredentialType().getValue());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), LocalCredentialDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        if (Objects.nonNull(entityQuery.getTenantId())) {
            List<Long> principalIds = tenantMembershipService.listPrincipalIdsByTenantId(entityQuery.getTenantId());
            if (CollectionUtils.isEmpty(principalIds)) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(LocalCredentialDO::getPrincipalId, principalIds);
            }
        }
        return wrapper;
    }

    /**
     * Validate required fields (principal, login name, password) and populate the
     * normalized login name, password hash, algorithm, and defaults before create.
     *
     * @param entityBO the credential to prepare
     */
    private void prepareForCreate(LocalCredentialBO entityBO) {
        if (Objects.isNull(entityBO.getPrincipalId())) {
            throw new EmptyException("The principal id is empty");
        }
        if (StringUtils.isBlank(entityBO.getLoginName())) {
            throw new EmptyException("The login name is empty");
        }
        if (StringUtils.isBlank(entityBO.getRawPassword())) {
            throw new EmptyException("The password is empty");
        }
        entityBO.setCredentialType(CredentialTypeEnum.PASSWORD);
        entityBO.setLoginNameNormalized(normalize(entityBO.getLoginName()));
        String hash = PasswordUtil.encode(entityBO.getRawPassword());
        entityBO.setPasswordHash(hash);
        entityBO.setPasswordAlgorithm(PasswordUtil.algorithmOfHash(hash));
        entityBO.setPasswordUpdatedTime(LocalDateTime.now());
        entityBO.setFailedAttempts(Objects.requireNonNullElse(entityBO.getFailedAttempts(), 0));
        entityBO.setRequirePasswordChange(Objects.requireNonNullElse(entityBO.getRequirePasswordChange(),
                RequirePasswordChangeFlagEnum.REQUIRED));
        entityBO.setEnableFlag(Objects.requireNonNullElse(entityBO.getEnableFlag(), EnableFlagEnum.ENABLE));
    }

    /**
     * Backfill immutable and blank fields from the current record before update,
     * re-hashing the password only when a new raw password is supplied.
     *
     * @param entityBO the credential being updated
     * @param current  the persisted record
     */
    private void prepareForUpdate(LocalCredentialBO entityBO, LocalCredentialDO current) {
        if (Objects.isNull(entityBO.getPrincipalId())) {
            entityBO.setPrincipalId(current.getPrincipalId());
        }
        if (StringUtils.isBlank(entityBO.getLoginName())) {
            entityBO.setLoginName(current.getLoginName());
        }
        entityBO.setCredentialType(CredentialTypeEnum.PASSWORD);
        entityBO.setLoginNameNormalized(normalize(entityBO.getLoginName()));
        if (StringUtils.isBlank(entityBO.getRawPassword())) {
            entityBO.setPasswordHash(current.getPasswordHash());
            entityBO.setPasswordAlgorithm(PasswordUtil.algorithmOfHash(current.getPasswordHash()));
            entityBO.setPasswordUpdatedTime(current.getPasswordUpdatedTime());
        } else {
            String hash = PasswordUtil.encode(entityBO.getRawPassword());
            entityBO.setPasswordHash(hash);
            entityBO.setPasswordAlgorithm(PasswordUtil.algorithmOfHash(hash));
            entityBO.setPasswordUpdatedTime(LocalDateTime.now());
        }
        if (Objects.isNull(entityBO.getFailedAttempts())) {
            entityBO.setFailedAttempts(current.getFailedAttempts());
        }
        if (Objects.isNull(entityBO.getRequirePasswordChange())) {
            entityBO.setRequirePasswordChange(RequirePasswordChangeFlagEnum.ofIndex(current.getRequirePasswordChange()));
        }
        if (Objects.isNull(entityBO.getEnableFlag())) {
            entityBO.setEnableFlag(EnableFlagEnum.ofIndex(current.getEnableFlag()));
        }
    }

    /**
     * Check whether a credential is duplicated by normalized login name or by principal
     * id within the same credential type.
     *
     * @param entityBO       {@link LocalCredentialBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(LocalCredentialBO entityBO, boolean isUpdate, boolean throwException) {
        boolean duplicateLogin = existsDuplicate(Wrappers.<LocalCredentialDO>query().lambda()
                .eq(LocalCredentialDO::getCredentialType, entityBO.getCredentialType().getValue())
                .eq(LocalCredentialDO::getLoginNameNormalized, entityBO.getLoginNameNormalized())
                .last(QueryWrapperConstant.LIMIT_ONE), entityBO.getId(), isUpdate);
        boolean duplicatePrincipal = existsDuplicate(Wrappers.<LocalCredentialDO>query().lambda()
                .eq(LocalCredentialDO::getCredentialType, entityBO.getCredentialType().getValue())
                .eq(LocalCredentialDO::getPrincipalId, entityBO.getPrincipalId())
                .last(QueryWrapperConstant.LIMIT_ONE), entityBO.getId(), isUpdate);
        boolean duplicate = duplicateLogin || duplicatePrincipal;
        if (throwException && duplicate) {
            throw new DuplicateException("Local credential has been duplicated");
        }
        return duplicate;
    }

    /**
     * Return whether a query matches an existing credential, excluding the entity's own
     * id on update.
     *
     * @param wrapper  the query wrapper
     * @param id       the entity id (used for update exclusion)
     * @param isUpdate whether the operation is an update
     * @return true if a duplicate exists
     */
    private boolean existsDuplicate(LambdaQueryWrapper<LocalCredentialDO> wrapper, Long id, boolean isUpdate) {
        LocalCredentialDO one = localCredentialManager.getOne(wrapper);
        return Objects.nonNull(one) && (!isUpdate || !one.getId().equals(id));
    }

    /**
     * Get local credential data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link LocalCredentialDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private LocalCredentialDO getDOById(Long id, boolean throwException) {
        LocalCredentialDO entityDO = localCredentialManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Local credential does not exist");
        }
        return entityDO;
    }

    /**
     * Normalize a login name to trimmed lowercase for case-insensitive matching.
     *
     * @param loginName the raw login name
     * @return the normalized login name
     */
    private String normalize(String loginName) {
        return StringUtils.trimToEmpty(loginName).toLowerCase(Locale.ROOT);
    }

}
