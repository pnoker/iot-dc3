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

package io.github.pnoker.common.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.manager.dal.CommandManager;
import io.github.pnoker.common.manager.dal.DeviceManager;
import io.github.pnoker.common.manager.dal.EventManager;
import io.github.pnoker.common.manager.dal.PointManager;
import io.github.pnoker.common.manager.dal.ProfileManager;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.ProfileBuilder;
import io.github.pnoker.common.manager.entity.model.CommandDO;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.EventDO;
import io.github.pnoker.common.manager.entity.model.PointDO;
import io.github.pnoker.common.manager.entity.model.ProfileDO;
import io.github.pnoker.common.manager.entity.query.ProfileQuery;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.mapper.ProfileMapper;
import io.github.pnoker.common.manager.service.ProfileService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Business service implementation for profile operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileBuilder profileBuilder;

    private final ProfileManager profileManager;

    private final PointManager pointManager;

    private final CommandManager commandManager;

    private final EventManager eventManager;

    private final DeviceManager deviceManager;

    private final ProfileMapper profileMapper;

    private final DeviceMapper deviceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(ProfileBO entityBO) {
        entityBO.setProfileCode(null);
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create profile: profile has been duplicated");
        }

        ProfileDO entityDO = profileBuilder.buildDOByBO(entityBO);
        if (!profileManager.save(entityDO)) {
            throw new AddException("Failed to create profile");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getDOById(id, true);

        validateNoAssociations(id);

        if (!profileManager.removeById(id)) {
            throw new DeleteException("Failed to remove profile");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProfileBO entityBO) {
        ProfileDO current = getDOById(entityBO.getId(), true);
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        entityBO.setProfileCode(current.getProfileCode());

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update profile: profile has been duplicated");
        }

        ProfileDO entityDO = profileBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!profileManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update profile");
        }
    }

    @Override
    public ProfileBO getById(Long id) {
        ProfileDO entityDO = getDOById(id, true);
        return profileBuilder.buildBOByDO(entityDO);
    }

    @Override
    public ProfileBO getByNameAndType(Long tenantId, String name, ProfileTypeEnum type) {
        LambdaQueryWrapper<ProfileDO> wrapper = Wrappers.<ProfileDO>query().lambda();
        wrapper.eq(ProfileDO::getProfileName, name);
        wrapper.eq(ProfileDO::getProfileTypeFlag, type);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ProfileDO entityDO = profileManager.getOne(wrapper);
        return profileBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<ProfileBO> listByIds(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ProfileDO> entityDOList = profileManager.listByIds(ids);
        return profileBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<ProfileBO> listByDeviceId(Long deviceId) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getId, deviceId);
        DeviceDO deviceDO = deviceMapper.selectOne(wrapper);
        if (Objects.isNull(deviceDO) || Objects.isNull(deviceDO.getProfileId())) {
            return Collections.emptyList();
        }
        ProfileBO profile = getById(deviceDO.getProfileId());
        if (Objects.isNull(profile) || !Objects.equals(deviceDO.getTenantId(), profile.getTenantId())) {
            return Collections.emptyList();
        }
        return List.of(profile);
    }

    @Override
    public Page<ProfileBO> list(ProfileQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<ProfileDO> entityPageDO = profileMapper.selectPageWithDevice(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery), entityQuery.getDeviceId());
        return profileBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * @param entityQuery {@link ProfileQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<ProfileDO> fuzzyQuery(ProfileQuery entityQuery) {
        QueryWrapper<ProfileDO> wrapper = Wrappers.query();
        wrapper.eq("dp.deleted", 0);
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getProfileName()), "dp.profile_name",
                entityQuery.getProfileName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getProfileCode()), "dp.profile_code",
                entityQuery.getProfileCode());
        wrapper.eq(Objects.nonNull(entityQuery.getProfileShareFlag()), "dp.profile_share_flag",
                Objects.isNull(entityQuery.getProfileShareFlag()) ? null
                        : entityQuery.getProfileShareFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getProfileTypeFlag()), "dp.profile_type_flag",
                Objects.isNull(entityQuery.getProfileTypeFlag()) ? null : entityQuery.getProfileTypeFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), "dp.enable_flag",
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), "dp.version", entityQuery.getVersion());
        wrapper.exists(FieldUtil.isValidIdField(entityQuery.getGroupId()),
                "select 1 from dc3_group_bind dgb where dgb.deleted = 0 "
                        + "and dgb.tenant_id = dp.tenant_id "
                        + "and dgb.entity_type_flag = {0} "
                        + "and dgb.entity_id = dp.id "
                        + "and dgb.group_id = {1}",
                EntityTypeEnum.PROFILE.getIndex(), entityQuery.getGroupId());
        wrapper.exists(FieldUtil.isValidIdField(entityQuery.getLabelId()),
                "select 1 from dc3_label_bind dlb where dlb.deleted = 0 "
                        + "and dlb.tenant_id = dp.tenant_id "
                        + "and dlb.entity_type_flag = {0} "
                        + "and dlb.entity_id = dp.id "
                        + "and dlb.label_id = {1}",
                EntityTypeEnum.PROFILE.getIndex(), entityQuery.getLabelId());
        return wrapper.lambda();
    }

    /**
     * @param entityBO {@link ProfileBO}
     * @param isUpdate
     * @return
     */
    private boolean checkDuplicate(ProfileBO entityBO, boolean isUpdate) {
        if (StringUtils.isEmpty(entityBO.getProfileName())) {
            return false;
        }
        LambdaQueryWrapper<ProfileDO> wrapper = Wrappers.<ProfileDO>query().lambda();
        wrapper.eq(ProfileDO::getProfileName, entityBO.getProfileName());
        wrapper.eq(Objects.nonNull(entityBO.getProfileTypeFlag()), ProfileDO::getProfileTypeFlag, entityBO.getProfileTypeFlag());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ProfileDO one = profileManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    private void validateNoAssociations(Long profileId) {
        long deviceCount = deviceManager.count(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getProfileId, profileId));
        if (deviceCount > 0) {
            throw new AssociatedException("Failed to remove profile: some devices still reference the profile");
        }

        long pointCount = pointManager.count(Wrappers.<PointDO>lambdaQuery().eq(PointDO::getProfileId, profileId));
        if (pointCount > 0) {
            throw new AssociatedException("Failed to remove profile: some points exist in the profile");
        }

        long commandCount = commandManager.count(Wrappers.<CommandDO>lambdaQuery().eq(CommandDO::getProfileId, profileId));
        if (commandCount > 0) {
            throw new AssociatedException("Failed to remove profile: some commands exist in the profile");
        }

        long eventCount = eventManager.count(Wrappers.<EventDO>lambdaQuery().eq(EventDO::getProfileId, profileId));
        if (eventCount > 0) {
            throw new AssociatedException("Failed to remove profile: some events exist in the profile");
        }
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link ProfileDO}
     */
    private ProfileDO getDOById(Long id, boolean throwException) {
        ProfileDO entityDO = profileManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Profile does not exist");
        }
        return entityDO;
    }

}
