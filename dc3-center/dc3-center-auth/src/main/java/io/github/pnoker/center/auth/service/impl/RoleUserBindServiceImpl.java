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
import io.github.pnoker.center.auth.dal.RoleManager;
import io.github.pnoker.center.auth.dal.RoleUserBindManager;
import io.github.pnoker.center.auth.entity.bo.RoleBO;
import io.github.pnoker.center.auth.entity.bo.RoleUserBindBO;
import io.github.pnoker.center.auth.entity.builder.RoleBuilder;
import io.github.pnoker.center.auth.entity.builder.RoleUserBindBuilder;
import io.github.pnoker.center.auth.entity.model.RoleDO;
import io.github.pnoker.center.auth.entity.model.RoleUserBindDO;
import io.github.pnoker.center.auth.entity.query.RoleUserBindQuery;
import io.github.pnoker.center.auth.service.RoleUserBindService;
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
public class RoleUserBindServiceImpl implements RoleUserBindService {

    @Resource
    private RoleUserBindBuilder roleUserBindBuilder;
    @Resource
    private RoleBuilder roleBuilder;

    @Resource
    private RoleUserBindManager roleUserBindManager;

    @Resource
    private RoleManager roleManager;


    @Override
    public RoleUserBindBO selectById(Long id) {
        return null;
    }

    @Override
    public Page<RoleUserBindBO> selectByPage(RoleUserBindQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RoleUserBindDO> entityPageDO = roleUserBindManager.page(PageUtil.page(entityQuery.getPage()), buildQueryWrapper(entityQuery));
        return roleUserBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public void save(RoleUserBindBO entityBO) {
        RoleUserBindDO entityDO = roleUserBindBuilder.buildDOByBO(entityBO);
        if (!roleUserBindManager.save(entityDO)) {
            throw new AddException("The role user bind add failed");
        }
    }

    @Override
    public void update(RoleUserBindBO entityBO) {
        selectById(entityBO.getId());
        RoleUserBindDO entityDO = roleUserBindBuilder.buildDOByBO(entityBO);
        if (!roleUserBindManager.updateById(entityDO)) {
            throw new UpdateException("The role user bind update failed");
        }
    }

    @Override
    public void remove(Long id) {
        selectById(id);
        if (!roleUserBindManager.removeById(id)) {
            throw new DeleteException("The role user bind delete failed");
        }
    }

    @Override
    public List<RoleBO> listRoleByTenantIdAndUserId(Long tenantId, Long userId) {
        LambdaQueryWrapper<RoleUserBindDO> wrapper = Wrappers.<RoleUserBindDO>query().lambda();
        wrapper.eq(RoleUserBindDO::getUserId, userId);
        List<RoleUserBindDO> roleUserBindBOS = roleUserBindManager.list(wrapper);
        if (CollUtil.isNotEmpty(roleUserBindBOS)) {
            List<RoleDO> roleBOS = roleManager.listByIds(roleUserBindBOS.stream().map(RoleUserBindDO::getRoleId)
                    .collect(Collectors.toList()));
            List<RoleDO> collect = roleBOS.stream().filter(e -> EnableFlagEnum.ENABLE.equals(e.getEnableFlag()) && tenantId.equals(e.getTenantId()))
                    .collect(Collectors.toList());
            return roleBuilder.buildBOListByDOList(collect);
        }

        return null;
    }

    private LambdaQueryWrapper<RoleUserBindDO> buildQueryWrapper(RoleUserBindQuery pageQuery) {
        LambdaQueryWrapper<RoleUserBindDO> wrapper = Wrappers.<RoleUserBindDO>query().lambda();
        if (ObjectUtil.isNotNull(pageQuery)) {
            wrapper.eq(ObjectUtil.isNotEmpty(pageQuery.getUserId()), RoleUserBindDO::getUserId, pageQuery.getUserId());
            wrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getRoleId()), RoleUserBindDO::getRoleId, pageQuery.getRoleId());
        }
        return wrapper;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link RoleUserBindDO}
     */
    private RoleUserBindDO getDOById(Long id, boolean throwException) {
        RoleUserBindDO entityDO = roleUserBindManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("角色用户绑定不存在");
        }
        return entityDO;
    }
}
