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
import io.github.pnoker.center.auth.entity.query.RoleUserBindPageQuery;
import io.github.pnoker.center.auth.mapper.RoleMapper;
import io.github.pnoker.center.auth.mapper.RoleUserBindMapper;
import io.github.pnoker.center.auth.service.RoleUserBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.model.Role;
import io.github.pnoker.common.model.RoleUserBind;
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
public class RoleUserBindServiceImpl implements RoleUserBindService {

    @Resource
    private RoleUserBindMapper roleUserBindMapper;

    @Resource
    private RoleMapper roleMapper;


    @Override
    public RoleUserBind selectById(Long id) {
        return null;
    }

    @Override
    public Page<RoleUserBind> selectByPage(RoleUserBindPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return roleUserBindMapper.selectPage(PageUtil.page(entityQuery.getPage()), buildQueryWrapper(entityQuery));
    }

    @Override
    public void save(RoleUserBind entityBO) {
        //todo check if exists
        if (roleUserBindMapper.insert(entityBO) < 1) {
            throw new AddException("The role user bind add failed");
        }
    }

    @Override
    public void update(RoleUserBind entityBO) {
        selectById(entityBO.getId());
        if (roleUserBindMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The role user bind update failed");
        }
    }

    @Override
    public void remove(Long id) {
        selectById(id);
        if (roleUserBindMapper.deleteById(id) < 1) {
            throw new DeleteException("The role user bind delete failed");
        }
    }

    @Override
    public List<Role> listRoleByTenantIdAndUserId(Long tenantId, Long userId) {
        LambdaQueryWrapper<RoleUserBind> queryWrapper = Wrappers.<RoleUserBind>query().lambda();
        queryWrapper.eq(RoleUserBind::getUserId, userId);
        List<RoleUserBind> roleUserBinds = roleUserBindMapper.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(roleUserBinds)) {
            List<Role> roles = roleMapper.selectBatchIds(roleUserBinds.stream().map(RoleUserBind::getRoleId)
                    .collect(Collectors.toList()));
            return roles.stream().filter(e -> EnableFlagEnum.ENABLE.equals(e.getEnableFlag()) && tenantId.equals(e.getTenantId()))
                    .collect(Collectors.toList());
        }

        return null;
    }

    public LambdaQueryWrapper<RoleUserBind> buildQueryWrapper(RoleUserBindPageQuery pageQuery) {
        LambdaQueryWrapper<RoleUserBind> queryWrapper = Wrappers.<RoleUserBind>query().lambda();
        if (ObjectUtil.isNotNull(pageQuery)) {
            queryWrapper.eq(ObjectUtil.isNotEmpty(pageQuery.getUserId()), RoleUserBind::getUserId, pageQuery.getUserId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getRoleId()), RoleUserBind::getRoleId, pageQuery.getRoleId());
        }
        return queryWrapper;
    }
}
