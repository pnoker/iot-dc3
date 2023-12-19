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

package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.bo.ResourceBO;
import io.github.pnoker.center.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.center.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.center.auth.entity.builder.RoleResourceBindBuilder;
import io.github.pnoker.center.auth.entity.model.ResourceDO;
import io.github.pnoker.center.auth.entity.model.RoleResourceBindDO;
import io.github.pnoker.center.auth.entity.query.RoleResourceBindQuery;
import io.github.pnoker.center.auth.manager.ResourceManager;
import io.github.pnoker.center.auth.manager.RoleResourceBindManager;
import io.github.pnoker.center.auth.service.RoleResourceBindService;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author linys
 * @since 2022.1.0
 */
@Slf4j
@Service
public class RoleResourceBindServiceImpl implements RoleResourceBindService {

    @Resource
    private ResourceBuilder resourceBuilder;
    @Resource
    private RoleResourceBindBuilder roleResourceBindBuilder;

    @Resource
    private RoleResourceBindManager roleResourceBindManager;
    @Resource
    private ResourceManager resourceManager;

    @Override
    public void save(RoleResourceBindBO entityBO) {
        RoleResourceBindDO entityDO = roleResourceBindBuilder.buildDOByBO(entityBO);
        if (!roleResourceBindManager.save(entityDO)) {
            throw new AddException("The tenant bind add failed");
        }
    }

    @Override
    public void remove(Long id) {
        selectById(id);
        if (!roleResourceBindManager.removeById(id)) {
            throw new DeleteException("The role resource bind delete failed");
        }
    }

    @Override
    public void update(RoleResourceBindBO entityBO) {
        selectById(entityBO.getId());
        RoleResourceBindDO entityDO = roleResourceBindBuilder.buildDOByBO(entityBO);
        if (!roleResourceBindManager.updateById(entityDO)) {
            throw new UpdateException("The role resource bind update failed");
        }
    }

    @Override
    public RoleResourceBindBO selectById(Long id) {
        return null;
    }

    @Override
    public Page<RoleResourceBindBO> selectByPage(RoleResourceBindQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RoleResourceBindDO> entityPageDO = roleResourceBindManager.page(PageUtil.page(entityQuery.getPage()), buildQueryWrapper(entityQuery));
        return roleResourceBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public List<ResourceBO> listResourceByRoleId(Long roleId) {
        LambdaQueryWrapper<RoleResourceBindDO> wrapper = Wrappers.<RoleResourceBindDO>query().lambda();
        wrapper.eq(RoleResourceBindDO::getRoleId, roleId);
        List<RoleResourceBindDO> entityDOS = roleResourceBindManager.list(wrapper);
        if (CollUtil.isNotEmpty(entityDOS)) {
            List<ResourceDO> resourceDOS = resourceManager.listByIds(entityDOS.stream()
                    .map(RoleResourceBindDO::getResourceId).collect(Collectors.toList()));
            List<ResourceDO> collect = resourceDOS.stream().filter(e -> EnableFlagEnum.ENABLE.equals(e.getEnableFlag()))
                    .collect(Collectors.toList());
            return resourceBuilder.buildBOListByDOList(collect);
        }

        return null;
    }

    private LambdaQueryWrapper<RoleResourceBindDO> buildQueryWrapper(RoleResourceBindQuery pageQuery) {
        LambdaQueryWrapper<RoleResourceBindDO> wrapper = Wrappers.<RoleResourceBindDO>query().lambda();
        if (ObjectUtil.isNotNull(pageQuery)) {
            wrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getRoleId()), RoleResourceBindDO::getResourceId, pageQuery.getRoleId());
            wrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getResourceId()), RoleResourceBindDO::getResourceId, pageQuery.getResourceId());
        }
        return wrapper;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link RoleResourceBindDO}
     */
    private RoleResourceBindDO getDOById(Long id, boolean throwException) {
        RoleResourceBindDO entityDO = roleResourceBindManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("角色资源绑定不存在");
        }
        return entityDO;
    }
}
