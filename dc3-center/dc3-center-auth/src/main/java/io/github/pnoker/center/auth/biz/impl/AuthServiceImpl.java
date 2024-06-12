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

package io.github.pnoker.center.auth.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.center.auth.biz.AuthService;
import io.github.pnoker.center.auth.entity.bean.Login;
import io.github.pnoker.center.auth.entity.bo.*;
import io.github.pnoker.center.auth.service.*;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.AuthUser;
import io.github.pnoker.common.utils.AuthUtil;
import io.github.pnoker.common.utils.DecodeUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理实现类
 *
 * @author linys
 * @since 2022.1.0
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private TenantService tenantService;

    @Resource
    private TenantBindService tenantBindService;

    @Resource
    private UserLoginService userLoginService;

    @Resource
    private UserPasswordService userPasswordService;

    @Resource
    private RoleUserBindService roleUserBindService;

    @Resource
    private RoleResourceBindService roleResourceBindService;

    @Override
    public UserLoginBO authenticateUser(Login login) {
        TenantBO tenantBO = tenantService.selectByCode(login.getTenant());
        if (Objects.isNull(tenantBO)) {
            throw new NotFoundException("Tenant[{}] does not exist", login.getTenant());
        }

        //todo checkUserLimit

        UserLoginBO userLogin = userLoginService.selectByLoginName(login.getName(), false);
        if (Objects.isNull(userLogin)) {
            throw new NotFoundException("User[{}] does not exist", login.getName());
        }

        TenantBindBO tenantBindBO = tenantBindService.selectByTenantIdAndUserId(tenantBO.getId(), userLogin.getUserId());
        if (Objects.isNull(tenantBindBO)) {
            throw new NotFoundException("租户, 用户信息不匹配");
        }

        UserPasswordBO userPasswordBO = userPasswordService.selectById(userLogin.getUserPasswordId());
        if (Objects.isNull(userPasswordBO)) {
            throw new NotFoundException("Password does not exist");
        }

        String saltValue = AuthUtil.getPasswordSalt(tenantBO.getId(), login.getName());
        if (CharSequenceUtil.isEmpty(saltValue)) {
            throw new NotFoundException("Salt does not exist");
        }

        String decodedPassword = DecodeUtil.md5(userPasswordBO.getLoginPassword() + saltValue);
        if (saltValue.equals(login.getSalt()) && decodedPassword.equals(login.getPassword())) {
            //create and save token
            String token = AuthUtil.createToken(tenantBO.getId(), login.getName(), saltValue);
            login.setToken(token);
            return userLogin;
        }

        //todo updateUserLimit
        return null;
    }

    @Override
    public AuthUser login(Login login) {
        //1. authenticate user
        UserLoginBO userLogin = authenticateUser(login);
        if (Objects.isNull(userLogin)) {
            throw new ServiceException("认证失败！请重试");
        }

        //2. save AuthUser
        AuthUser authUser = new AuthUser();
        authUser.setUserId(userLogin.getUserId());
        authUser.setUserName(userLogin.getLoginName());
        TenantBO tenantBO = tenantService.selectByCode(login.getTenant());
        authUser.setTenantId(tenantBO.getId());

        //2.1 roles
        List<RoleBO> roleBOList = roleUserBindService.listRoleByTenantIdAndUserId(tenantBO.getId(), userLogin.getUserId());
        if (CollUtil.isEmpty(roleBOList)) {
            throw new ServiceException("请先为用户{}分配角色", login.getName());
        }
        Set<String> roleCodeSet = roleBOList.stream().map(RoleBO::getRoleCode).collect(Collectors.toSet());
        authUser.setRoleCodeSet(roleCodeSet);

        //2.2 resources
        Set<ResourceBO> resourceBOSet = new HashSet<>(4);
        for (RoleBO roleBO : roleBOList) {
            List<ResourceBO> resourceBOList = roleResourceBindService.listResourceByRoleId(roleBO.getId());
            resourceBOSet.addAll(resourceBOList);
        }
        if (CollUtil.isEmpty(resourceBOSet)) {
            throw new ServiceException("请先为用户{}分配权限", login.getName());
        }
        Set<String> resourceCodeSet = resourceBOSet.stream().map(ResourceBO::getResourceCode).collect(Collectors.toSet());
        authUser.setResourceCodeSet(resourceCodeSet);

        AuthUtil.saveTokenToAuthUserMap(login.getToken(), authUser);
        return authUser;
    }

    @Override
    public void logout() {
        AuthUtil.logout();
    }
}
