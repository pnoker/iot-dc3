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

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.biz.DriverNotifyService;
import io.github.pnoker.center.manager.dal.PointManager;
import io.github.pnoker.center.manager.dal.ProfileBindManager;
import io.github.pnoker.center.manager.dal.ProfileManager;
import io.github.pnoker.center.manager.entity.bo.ProfileBO;
import io.github.pnoker.center.manager.entity.builder.ProfileBuilder;
import io.github.pnoker.center.manager.entity.model.PointDO;
import io.github.pnoker.center.manager.entity.model.ProfileBindDO;
import io.github.pnoker.center.manager.entity.model.ProfileDO;
import io.github.pnoker.center.manager.entity.query.ProfileQuery;
import io.github.pnoker.center.manager.mapper.ProfileMapper;
import io.github.pnoker.center.manager.service.ProfileService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeFlagEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import io.github.pnoker.common.utils.UserHeaderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ProfileService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    @Resource
    private ProfileBuilder profileBuilder;

    @Resource
    private ProfileManager profileManager;
    @Resource
    private ProfileBindManager profileBindManager;
    @Resource
    private PointManager pointManager;

    @Resource
    private ProfileMapper profileMapper;

    @Resource
    private DriverNotifyService driverNotifyService;

    @Override
    public void save(ProfileBO entityBO) {
        checkDuplicate(entityBO, false, true);

        ProfileDO entityDO = profileBuilder.buildDOByBO(entityBO);
        if (!profileManager.save(entityDO)) {
            throw new AddException("模板创建失败");
        }
    }


    @Override
    public void remove(Long id) {
        ProfileDO entityDO = getDOById(id, true);

        // 删除模板之前需要检查该模板是否存在关联
        LambdaQueryChainWrapper<PointDO> wrapper = pointManager.lambdaQuery().eq(PointDO::getProfileId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("模板删除失败，该模板下存在位号");
        }

        if (!profileManager.removeById(id)) {
            throw new DeleteException("The profile delete failed");
        }

        ProfileBO entityBO = profileBuilder.buildBOByDO(entityDO);
        driverNotifyService.notifyProfile(MetadataCommandTypeEnum.DELETE, entityBO);
    }

    @Override
    public void update(ProfileBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        ProfileDO entityDO = profileBuilder.buildDOByBO(entityBO);
        entityBO.setOperateTime(null);
        if (!profileManager.updateById(entityDO)) {
            throw new UpdateException("模板更新失败");
        }

        entityDO = profileManager.getById(entityDO.getId());
        entityBO = profileBuilder.buildBOByDO(entityDO);
        driverNotifyService.notifyProfile(MetadataCommandTypeEnum.UPDATE, entityBO);
    }

    @Override
    public ProfileBO selectById(Long id) {
        ProfileDO entityDO = getDOById(id, true);
        return profileBuilder.buildBOByDO(entityDO);
    }

    @Override
    public ProfileBO selectByNameAndType(String name, ProfileTypeFlagEnum type) {
        LambdaQueryWrapper<ProfileDO> wrapper = Wrappers.<ProfileDO>query().lambda();
        wrapper.eq(ProfileDO::getProfileName, name);
        wrapper.eq(ProfileDO::getProfileTypeFlag, type);
        wrapper.eq(ProfileDO::getTenantId, UserHeaderUtil.getUserHeader().getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ProfileDO entityDO = profileManager.getOne(wrapper);
        return profileBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<ProfileBO> selectByIds(Set<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ProfileDO> entityDOS = profileManager.listByIds(ids);
        return profileBuilder.buildBOListByDOList(entityDOS);
    }

    @Override
    public List<ProfileBO> selectByDeviceId(Long deviceId) {
        LambdaQueryChainWrapper<ProfileBindDO> wrapper = profileBindManager.lambdaQuery().eq(ProfileBindDO::getDeviceId, deviceId);
        List<ProfileBindDO> entityDOS = wrapper.list();
        Set<Long> profileIds = entityDOS.stream().map(ProfileBindDO::getProfileId).collect(Collectors.toSet());
        return selectByIds(profileIds);
    }

    @Override
    public Page<ProfileBO> selectByPage(ProfileQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<ProfileDO> entityPageDO = profileMapper.selectPageWithDevice(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery), entityQuery.getDeviceId());
        return profileBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<ProfileDO> fuzzyQuery(ProfileQuery entityQuery) {
        QueryWrapper<ProfileDO> wrapper = Wrappers.query();
        wrapper.eq("dp.deleted", 0);
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getProfileName()), "dp.profile_name", entityQuery.getProfileName());
        wrapper.eq(CharSequenceUtil.isNotEmpty(entityQuery.getProfileCode()), "dp.profile_code", entityQuery.getProfileCode());
        wrapper.eq(ObjectUtil.isNotNull(entityQuery.getProfileShareFlag()), "dp.profile_share_flag", entityQuery.getProfileShareFlag());
        wrapper.eq(ObjectUtil.isNotNull(entityQuery.getEnableFlag()), "dp.enable_flag", entityQuery.getEnableFlag());
        wrapper.eq("dp.tenant_id", entityQuery.getTenantId());
        return wrapper.lambda();
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link ProfileBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(ProfileBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<ProfileDO> wrapper = Wrappers.<ProfileDO>query().lambda();
        wrapper.eq(ProfileDO::getProfileName, entityBO.getProfileName());
        wrapper.eq(ProfileDO::getProfileCode, entityBO.getProfileCode());
        wrapper.eq(ProfileDO::getProfileTypeFlag, entityBO.getProfileTypeFlag());
        wrapper.eq(ProfileDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ProfileDO one = profileManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("模板重复");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link ProfileDO}
     */
    private ProfileDO getDOById(Long id, boolean throwException) {
        ProfileDO entityDO = profileManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("模板不存在");
        }
        return entityDO;
    }

}
