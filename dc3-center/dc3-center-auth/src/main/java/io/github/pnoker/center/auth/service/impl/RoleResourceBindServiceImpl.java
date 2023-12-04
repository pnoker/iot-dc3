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
import io.github.pnoker.center.auth.entity.query.RoleResourceBindPageQuery;
import io.github.pnoker.center.auth.mapper.ResourceMapper;
import io.github.pnoker.center.auth.mapper.RoleResourceBindMapper;
import io.github.pnoker.center.auth.service.RoleResourceBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.model.RoleResourceBind;
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
    private RoleResourceBindMapper bindMapper;

    @Resource
    private ResourceMapper resourceMapper;

    @Override
    public void save(RoleResourceBind entityBO) {
        //todo check if exists
        if (bindMapper.insert(entityBO) < 1) {
            throw new AddException("The tenant bind add failed");
        }
    }

    @Override
    public void remove(Long id) {
        selectById(id);
        if (bindMapper.deleteById(id) < 1) {
            throw new DeleteException("The role resource bind delete failed");
        }
    }

    @Override
    public void update(RoleResourceBind entityBO) {
        selectById(entityBO.getId());
        if (bindMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The role resource bind update failed");
        }
    }

    @Override
    public RoleResourceBind selectById(Long id) {
        return null;
    }

    @Override
    public Page<RoleResourceBind> selectByPage(RoleResourceBindPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return bindMapper.selectPage(PageUtil.page(entityQuery.getPage()), buildQueryWrapper(entityQuery));
    }

    @Override
    public List<io.github.pnoker.common.model.Resource> listResourceByRoleId(Long RoleId) {
        LambdaQueryWrapper<RoleResourceBind> queryWrapper = Wrappers.<RoleResourceBind>query().lambda();
        queryWrapper.eq(RoleResourceBind::getRoleId, RoleId);
        List<RoleResourceBind> roleResourceBinds = bindMapper.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(roleResourceBinds)) {
            List<io.github.pnoker.common.model.Resource> resources = resourceMapper.selectBatchIds(roleResourceBinds.stream()
                    .map(RoleResourceBind::getResourceId).collect(Collectors.toList()));
            return resources.stream().filter(e -> EnableFlagEnum.ENABLE.equals(e.getEnableFlag()))
                    .collect(Collectors.toList());
        }

        return null;
    }

    private LambdaQueryWrapper<RoleResourceBind> buildQueryWrapper(RoleResourceBindPageQuery pageQuery) {
        LambdaQueryWrapper<RoleResourceBind> queryWrapper = Wrappers.<RoleResourceBind>query().lambda();
        if (ObjectUtil.isNotNull(pageQuery)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getRoleId()), RoleResourceBind::getResourceId, pageQuery.getRoleId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getResourceId()), RoleResourceBind::getResourceId, pageQuery.getResourceId());
        }
        return queryWrapper;
    }
}
