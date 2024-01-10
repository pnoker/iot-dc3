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

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.dal.ProfileBindManager;
import io.github.pnoker.center.manager.entity.bo.ProfileBindBO;
import io.github.pnoker.center.manager.entity.builder.ProfileBindBuilder;
import io.github.pnoker.center.manager.entity.model.ProfileBindDO;
import io.github.pnoker.center.manager.entity.query.ProfileBindQuery;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ProfileBindService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ProfileBindServiceImpl implements ProfileBindService {

    @Resource
    private ProfileBindBuilder profileBindBuilder;

    @Resource
    private ProfileBindManager profileBindManager;

    @Override
    public void save(ProfileBindBO entityBO) {
        checkDuplicate(entityBO, false, true);

        ProfileBindDO entityDO = profileBindBuilder.buildDOByBO(entityBO);
        if (!profileBindManager.save(entityDO)) {
            throw new AddException("模板绑定创建失败");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!profileBindManager.removeById(id)) {
            throw new DeleteException("模板绑定删除失败");
        }
    }

    @Override
    public Boolean removeByDeviceId(Long deviceId) {
        LambdaQueryWrapper<ProfileBindDO> wrapper = Wrappers.<ProfileBindDO>query().lambda();
        wrapper.eq(ProfileBindDO::getDeviceId, deviceId);
        return profileBindManager.remove(wrapper);
    }

    @Override
    public Boolean removeByDeviceIdAndProfileId(Long deviceId, Long profileId) {
        LambdaQueryWrapper<ProfileBindDO> wrapper = Wrappers.<ProfileBindDO>query().lambda();
        wrapper.eq(ProfileBindDO::getDeviceId, deviceId);
        wrapper.eq(ProfileBindDO::getProfileId, profileId);
        return profileBindManager.remove(wrapper);
    }

    @Override
    public void update(ProfileBindBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        ProfileBindDO entityDO = profileBindBuilder.buildDOByBO(entityBO);
        entityBO.setOperateTime(null);
        if (!profileBindManager.updateById(entityDO)) {
            throw new UpdateException("模板绑定更新失败");
        }
    }

    @Override
    public ProfileBindBO selectById(Long id) {
        ProfileBindDO entityDO = getDOById(id, true);
        return profileBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public ProfileBindBO selectByDeviceIdAndProfileId(Long deviceId, Long profileId) {
        LambdaQueryChainWrapper<ProfileBindDO> wrapper = profileBindManager.lambdaQuery()
                .eq(ProfileBindDO::getDeviceId, deviceId)
                .eq(ProfileBindDO::getProfileId, profileId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        ProfileBindDO entityDO = wrapper.one();
        return profileBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Set<Long> selectDeviceIdsByProfileId(Long profileId) {
        LambdaQueryChainWrapper<ProfileBindDO> wrapper = profileBindManager.lambdaQuery()
                .eq(ProfileBindDO::getProfileId, profileId);
        List<ProfileBindDO> entityDOS = wrapper.list();
        return entityDOS.stream().map(ProfileBindDO::getDeviceId).collect(Collectors.toSet());
    }

    @Override
    public Set<Long> selectProfileIdsByDeviceId(Long deviceId) {
        LambdaQueryChainWrapper<ProfileBindDO> wrapper = profileBindManager.lambdaQuery()
                .eq(ProfileBindDO::getDeviceId, deviceId);
        List<ProfileBindDO> entityDOS = wrapper.list();
        return entityDOS.stream().map(ProfileBindDO::getProfileId).collect(Collectors.toSet());
    }

    @Override
    public Page<ProfileBindBO> selectByPage(ProfileBindQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<ProfileBindDO> entityPageDO = profileBindManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return profileBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<ProfileBindDO> fuzzyQuery(ProfileBindQuery entityQuery) {
        LambdaQueryWrapper<ProfileBindDO> wrapper = Wrappers.<ProfileBindDO>query().lambda();
        wrapper.eq(ObjectUtil.isNotEmpty(entityQuery.getProfileId()), ProfileBindDO::getProfileId, entityQuery.getProfileId());
        wrapper.eq(ObjectUtil.isNotEmpty(entityQuery.getDeviceId()), ProfileBindDO::getDeviceId, entityQuery.getDeviceId());
        wrapper.eq(ProfileBindDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link ProfileBindBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(ProfileBindBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<ProfileBindDO> wrapper = Wrappers.<ProfileBindDO>query().lambda();
        wrapper.eq(ProfileBindDO::getDeviceId, entityBO.getDeviceId());
        wrapper.eq(ProfileBindDO::getProfileId, entityBO.getProfileId());
        wrapper.eq(ProfileBindDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ProfileBindDO one = profileBindManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("模板绑定重复");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link ProfileBindDO}
     */
    private ProfileBindDO getDOById(Long id, boolean throwException) {
        ProfileBindDO entityDO = profileBindManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("模板绑定不存在");
        }
        return entityDO;
    }

}
