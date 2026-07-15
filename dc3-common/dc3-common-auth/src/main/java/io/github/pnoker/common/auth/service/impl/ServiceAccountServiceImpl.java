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
import io.github.pnoker.common.auth.dal.PrincipalManager;
import io.github.pnoker.common.auth.dal.ServiceAccountManager;
import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.builder.ServiceAccountBuilder;
import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.common.auth.entity.model.ServiceAccountDO;
import io.github.pnoker.common.auth.entity.query.ServiceAccountQuery;
import io.github.pnoker.common.auth.service.ServiceAccountService;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.MembershipStatusEnum;
import io.github.pnoker.common.enums.PrincipalSourceTypeEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Service account service implementation.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceAccountServiceImpl implements ServiceAccountService {

    private final ServiceAccountBuilder serviceAccountBuilder;

    private final ServiceAccountManager serviceAccountManager;

    private final PrincipalManager principalManager;

    private final TenantMembershipService tenantMembershipService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(ServiceAccountBO entityBO) {
        prepareForCreate(entityBO);
        checkDuplicate(entityBO, false, true);

        PrincipalDO principal = buildPrincipal(entityBO);
        if (!principalManager.save(principal)) {
            throw new AddException("Failed to create service account principal");
        }
        entityBO.setPrincipalId(principal.getId());

        ServiceAccountDO entityDO = serviceAccountBuilder.buildDOByBO(entityBO);
        if (!serviceAccountManager.save(entityDO)) {
            throw new AddException("Failed to create service account");
        }

        TenantMembershipBO membership = buildMembership(entityBO);
        tenantMembershipService.add(membership);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ServiceAccountDO current = getDOById(id, true);
        TenantMembershipBO membership = tenantMembershipService.getByTenantIdAndPrincipalId(current.getTenantId(),
                current.getPrincipalId());
        if (!serviceAccountManager.removeById(id)) {
            throw new DeleteException("Failed to remove service account");
        }
        if (Objects.nonNull(membership)) {
            tenantMembershipService.delete(membership.getId());
        }
        principalManager.removeById(current.getPrincipalId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ServiceAccountBO entityBO) {
        ServiceAccountDO current = getDOById(entityBO.getId(), true);
        prepareForUpdate(entityBO, current);
        checkDuplicate(entityBO, true, true);

        ServiceAccountDO entityDO = serviceAccountBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!serviceAccountManager.updateById(entityDO)) {
            throw new UpdateException("The service account update failed");
        }
        PrincipalDO principal = principalManager.getById(entityBO.getPrincipalId());
        if (Objects.nonNull(principal)) {
            principal.setPrincipalName(principalName(entityBO.getTenantId(), entityBO.getServiceAccountName()));
            principal.setDisplayName(entityBO.getServiceAccountName());
            principal.setEnableFlag(entityBO.getEnableFlag().getIndex());
            principal.setOperatorId(entityBO.getOperatorId());
            principal.setOperatorName(entityBO.getOperatorName());
            principal.setOperateTime(null);
            principalManager.updateById(principal);
        }
    }

    @Override
    public ServiceAccountBO getById(Long id) {
        return serviceAccountBuilder.buildBOByDO(getDOById(id, true));
    }

    @Override
    public Page<ServiceAccountBO> list(ServiceAccountQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<ServiceAccountDO> page = serviceAccountManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return serviceAccountBuilder.buildBOPageByDOPage(page);
    }

    @Override
    public ServiceAccountBO getByPrincipalId(Long principalId, boolean throwException) {
        LambdaQueryWrapper<ServiceAccountDO> wrapper = Wrappers.<ServiceAccountDO>query().lambda();
        wrapper.eq(ServiceAccountDO::getPrincipalId, principalId);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ServiceAccountDO entityDO = serviceAccountManager.getOne(wrapper);
        if (Objects.isNull(entityDO)) {
            if (throwException) {
                throw new NotFoundException("Service account does not exist");
            }
            return null;
        }
        return serviceAccountBuilder.buildBOByDO(entityDO);
    }

    /**
     * Build fuzzy query wrapper for service account search.
     *
     * @param entityQuery {@link ServiceAccountQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link ServiceAccountDO}
     */
    private LambdaQueryWrapper<ServiceAccountDO> fuzzyQuery(ServiceAccountQuery entityQuery) {
        LambdaQueryWrapper<ServiceAccountDO> wrapper = Wrappers.<ServiceAccountDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getTenantId()), ServiceAccountDO::getTenantId,
                entityQuery.getTenantId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getPrincipalId()), ServiceAccountDO::getPrincipalId,
                entityQuery.getPrincipalId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getOwnerPrincipalId()), ServiceAccountDO::getOwnerPrincipalId,
                entityQuery.getOwnerPrincipalId());
        wrapper.like(StringUtils.isNotBlank(entityQuery.getServiceAccountName()),
                ServiceAccountDO::getServiceAccountName, entityQuery.getServiceAccountName());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), ServiceAccountDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        return wrapper;
    }

    /**
     * Validate required fields (tenant, name, owner, purpose) and default the enable
     * flag before create.
     *
     * @param entityBO the service account to prepare
     */
    private void prepareForCreate(ServiceAccountBO entityBO) {
        if (Objects.isNull(entityBO.getTenantId())) {
            throw new EmptyException("The tenant id is empty");
        }
        if (StringUtils.isBlank(entityBO.getServiceAccountName())) {
            throw new EmptyException("The service account name is empty");
        }
        if (Objects.isNull(entityBO.getOwnerPrincipalId())) {
            throw new EmptyException("The owner principal id is empty");
        }
        if (StringUtils.isBlank(entityBO.getPurpose())) {
            throw new EmptyException("The service account purpose is empty");
        }
        entityBO.setEnableFlag(Objects.requireNonNullElse(entityBO.getEnableFlag(), EnableFlagEnum.ENABLE));
    }

    /**
     * Backfill immutable and blank fields from the current record before update.
     *
     * @param entityBO the service account being updated
     * @param current  the persisted record
     */
    private void prepareForUpdate(ServiceAccountBO entityBO, ServiceAccountDO current) {
        entityBO.setPrincipalId(current.getPrincipalId());
        entityBO.setTenantId(current.getTenantId());
        if (StringUtils.isBlank(entityBO.getServiceAccountName())) {
            entityBO.setServiceAccountName(current.getServiceAccountName());
        }
        if (Objects.isNull(entityBO.getOwnerPrincipalId())) {
            entityBO.setOwnerPrincipalId(current.getOwnerPrincipalId());
        }
        if (StringUtils.isBlank(entityBO.getPurpose())) {
            entityBO.setPurpose(current.getPurpose());
        }
        if (Objects.isNull(entityBO.getEnableFlag())) {
            entityBO.setEnableFlag(EnableFlagEnum.ofIndex(current.getEnableFlag()));
        }
    }

    /**
     * Build the linked Principal DO from a service account BO.
     *
     * @param entityBO the service account
     * @return the assembled Principal DO
     */
    private PrincipalDO buildPrincipal(ServiceAccountBO entityBO) {
        PrincipalDO principal = new PrincipalDO();
        principal.setPrincipalType(PrincipalTypeEnum.SERVICE_ACCOUNT.getValue());
        principal.setPrincipalName(principalName(entityBO.getTenantId(), entityBO.getServiceAccountName()));
        principal.setDisplayName(entityBO.getServiceAccountName());
        principal.setSourceType(PrincipalSourceTypeEnum.LOCAL.getValue());
        principal.setEnableFlag(entityBO.getEnableFlag().getIndex());
        principal.setLockedFlag(EnableFlagEnum.ENABLE.getIndex());
        principal.setCreatorId(entityBO.getCreatorId());
        principal.setCreatorName(entityBO.getCreatorName());
        principal.setOperatorId(entityBO.getOperatorId());
        principal.setOperatorName(entityBO.getOperatorName());
        return principal;
    }

    /**
     * Build the tenant membership BO linking the service account's principal to its
     * tenant.
     *
     * @param entityBO the service account
     * @return the assembled membership BO
     */
    private TenantMembershipBO buildMembership(ServiceAccountBO entityBO) {
        TenantMembershipBO membership = new TenantMembershipBO();
        membership.setTenantId(entityBO.getTenantId());
        membership.setPrincipalId(entityBO.getPrincipalId());
        membership.setPrincipalType(PrincipalTypeEnum.SERVICE_ACCOUNT);
        membership.setMembershipStatus(MembershipStatusEnum.ACTIVE);
        membership.setCreatorId(entityBO.getCreatorId());
        membership.setCreatorName(entityBO.getCreatorName());
        membership.setOperatorId(entityBO.getOperatorId());
        membership.setOperatorName(entityBO.getOperatorName());
        return membership;
    }

    /**
     * Check whether a service account is duplicated by tenant and name.
     *
     * @param entityBO       {@link ServiceAccountBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(ServiceAccountBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<ServiceAccountDO> wrapper = Wrappers.<ServiceAccountDO>query().lambda();
        wrapper.eq(ServiceAccountDO::getTenantId, entityBO.getTenantId());
        wrapper.eq(ServiceAccountDO::getServiceAccountName, entityBO.getServiceAccountName());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ServiceAccountDO one = serviceAccountManager.getOne(wrapper);
        boolean duplicate = Objects.nonNull(one) && (!isUpdate || !one.getId().equals(entityBO.getId()));
        if (throwException && duplicate) {
            throw new DuplicateException("Service account has been duplicated");
        }
        return duplicate;
    }

    /**
     * Get service account data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link ServiceAccountDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private ServiceAccountDO getDOById(Long id, boolean throwException) {
        ServiceAccountDO entityDO = serviceAccountManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Service account does not exist");
        }
        return entityDO;
    }

    /**
     * Build the unique principal name for a service account from tenant id and name.
     *
     * @param tenantId           tenant id
     * @param serviceAccountName service account name
     * @return the composite principal name
     */
    private String principalName(Long tenantId, String serviceAccountName) {
        return tenantId + ":" + serviceAccountName;
    }

}
