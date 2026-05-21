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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.manager.dal.ProfileBindManager;
import io.github.pnoker.common.manager.entity.bo.ProfileBindBO;
import io.github.pnoker.common.manager.entity.builder.ProfileBindBuilder;
import io.github.pnoker.common.manager.entity.model.ProfileBindDO;
import io.github.pnoker.common.manager.entity.query.ProfileBindQuery;
import io.github.pnoker.common.manager.service.ProfileBindService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Business service implementation for profile binding operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileBindServiceImpl implements ProfileBindService {

    private final ProfileBindBuilder profileBindBuilder;

    private final ProfileBindManager profileBindManager;

    @Override
    public void add(ProfileBindBO entityBO) {
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create profile bind: profile bind has been duplicated");
        }

        ProfileBindDO entityDO = profileBindBuilder.buildDOByBO(entityBO);
        if (!profileBindManager.save(entityDO)) {
            throw new AddException("Failed to create profile bind");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!profileBindManager.removeById(id)) {
            throw new DeleteException("Failed to remove profile bind");
        }
    }

    @Override
    public void removeByDeviceId(Long deviceId) {
        LambdaQueryWrapper<ProfileBindDO> wrapper = Wrappers.<ProfileBindDO>query().lambda();
        wrapper.eq(ProfileBindDO::getDeviceId, deviceId);
        if (profileBindManager.count(wrapper) == 0) {
            return;
        }
        if (!profileBindManager.remove(wrapper)) {
            throw new DeleteException("Failed to remove profile bind by deviceId");
        }
    }

    @Override
    public void removeByDeviceIdAndProfileId(Long deviceId, Long profileId) {
        LambdaQueryWrapper<ProfileBindDO> wrapper = Wrappers.<ProfileBindDO>query().lambda();
        wrapper.eq(ProfileBindDO::getDeviceId, deviceId);
        wrapper.eq(ProfileBindDO::getProfileId, profileId);
        if (profileBindManager.count(wrapper) == 0) {
            return;
        }
        if (!profileBindManager.remove(wrapper)) {
            throw new DeleteException("Failed to remove profile bind by deviceId and profileId");
        }
    }

    @Override
    public void update(ProfileBindBO entityBO) {
        getDOById(entityBO.getId(), true);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update profile bind: profile bind has been duplicated");
        }

        ProfileBindDO entityDO = profileBindBuilder.buildDOByBO(entityBO);
        entityBO.setOperateTime(null);
        if (!profileBindManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update profile bind");
        }
    }

    @Override
    public ProfileBindBO getById(Long id) {
        ProfileBindDO entityDO = getDOById(id, true);
        return profileBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public ProfileBindBO getByDeviceIdAndProfileId(Long deviceId, Long profileId) {
        LambdaQueryChainWrapper<ProfileBindDO> wrapper = profileBindManager.lambdaQuery()
                .eq(ProfileBindDO::getDeviceId, deviceId)
                .eq(ProfileBindDO::getProfileId, profileId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        ProfileBindDO entityDO = wrapper.one();
        return profileBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<Long> listDeviceIdsByProfileId(Long profileId) {
        LambdaQueryChainWrapper<ProfileBindDO> wrapper = profileBindManager.lambdaQuery()
                .eq(ProfileBindDO::getProfileId, profileId)
                .select(ProfileBindDO::getDeviceId);
        return wrapper.list().stream().map(ProfileBindDO::getDeviceId).toList();
    }

    @Override
    public List<Long> listProfileIdsByDeviceId(Long deviceId) {
        LambdaQueryChainWrapper<ProfileBindDO> wrapper = profileBindManager.lambdaQuery()
                .eq(ProfileBindDO::getDeviceId, deviceId)
                .select(ProfileBindDO::getProfileId);
        return wrapper.list().stream().map(ProfileBindDO::getProfileId).toList();
    }

    @Override
    public Page<ProfileBindBO> list(ProfileBindQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<ProfileBindDO> entityPageDO = profileBindManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return profileBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * @param entityQuery {@link ProfileBindQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<ProfileBindDO> fuzzyQuery(ProfileBindQuery entityQuery) {
        LambdaQueryWrapper<ProfileBindDO> wrapper = Wrappers.<ProfileBindDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getProfileId()), ProfileBindDO::getProfileId,
                entityQuery.getProfileId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDeviceId()), ProfileBindDO::getDeviceId,
                entityQuery.getDeviceId());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), ProfileBindDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * @param entityBO {@link ProfileBindBO}
     * @param isUpdate
     * @return
     */
    private boolean checkDuplicate(ProfileBindBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<ProfileBindDO> wrapper = Wrappers.<ProfileBindDO>query().lambda();
        wrapper.eq(ProfileBindDO::getDeviceId, entityBO.getDeviceId());
        wrapper.eq(ProfileBindDO::getProfileId, entityBO.getProfileId());
        wrapper.eq(ProfileBindDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ProfileBindDO one = profileBindManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link ProfileBindDO}
     */
    private ProfileBindDO getDOById(Long id, boolean throwException) {
        ProfileBindDO entityDO = profileBindManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Profile bind does not exist");
        }
        return entityDO;
    }

}
