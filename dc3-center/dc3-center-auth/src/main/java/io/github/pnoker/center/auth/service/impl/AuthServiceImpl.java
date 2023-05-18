package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.center.auth.service.*;
import io.github.pnoker.common.entity.auth.Login;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.*;
import io.github.pnoker.common.utils.AuthUtil;
import io.github.pnoker.common.utils.DecodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理实现类
 *
 * @author linys
 * @since 2023.04.02
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
    public UserLogin authenticateUser(Login login) {
        Tenant tenant = tenantService.selectByCode(login.getTenant());
        if (ObjectUtil.isNull(tenant)) {
            throw new NotFoundException("租户{}不存在", login.getTenant());
        }

        //todo checkUserLimit

        UserLogin userLogin = userLoginService.selectByLoginName(login.getName(), false);
        if (ObjectUtil.isNull(userLogin)) {
            throw new NotFoundException("用户{}不存在", login.getName());
        }

        TenantBind tenantBind = tenantBindService.selectByTenantIdAndUserId(tenant.getId(), userLogin.getUserId());
        if (ObjectUtil.isNull(tenantBind)) {
            throw new NotFoundException("租户、用户信息不匹配");
        }

        UserPassword userPassword = userPasswordService.selectById(userLogin.getUserPasswordId());
        if (ObjectUtil.isNull(userPassword)) {
            throw new NotFoundException("密码不存在，请先设置密码");
        }

        String saltValue = AuthUtil.getPasswordSalt(tenant.getId(), login.getName());
        if (CharSequenceUtil.isEmpty(saltValue)) {
            throw new NotFoundException("密码盐不存在，请重新登录");
        }

        String decodedPassword = DecodeUtil.md5(userPassword.getLoginPassword() + saltValue);
        if (saltValue.equals(login.getSalt()) && decodedPassword.equals(login.getPassword())) {
            //create and save token
            String token = AuthUtil.createToken(tenant.getId(), login.getName(), saltValue);
            login.setToken(token);
            return userLogin;
        }

        //todo updateUserLimit
        return null;
    }

    @Override
    public AuthUser login(Login login) {
        //1. authenticate user
        UserLogin userLogin = authenticateUser(login);
        if (ObjectUtil.isNull(userLogin)) {
            throw new ServiceException("认证失败！请重试");
        }

        //2. save AuthUser
        AuthUser authUser = new AuthUser();
        authUser.setUserId(userLogin.getUserId());
        authUser.setUserName(userLogin.getLoginName());
        Tenant tenant = tenantService.selectByCode(login.getTenant());
        authUser.setTenantId(tenant.getId());

        //2.1 roles
        List<Role> roles = roleUserBindService.listRoleByTenantIdAndUserId(tenant.getId(), userLogin.getUserId());
        if (CollUtil.isEmpty(roles)) {
            throw new ServiceException("请先为用户{}分配角色", login.getName());
        }
        Set<String> roleCodeSet = roles.stream().map(Role::getRoleCode).collect(Collectors.toSet());
        authUser.setRoleCodeSet(roleCodeSet);

        //2.2 resources
        Set<io.github.pnoker.common.model.Resource> resourceSet = new HashSet<>();
        for (Role role : roles) {
            List<io.github.pnoker.common.model.Resource> resources = roleResourceBindService.listResourceByRoleId(role.getId());
            resourceSet.addAll(resources);
        }
        if (CollUtil.isEmpty(resourceSet)) {
            throw new ServiceException("请先为用户{}分配权限", login.getName());
        }
        Set<String> resourceCodeSet = resourceSet.stream().map(io.github.pnoker.common.model.Resource::getResourceCode).collect(Collectors.toSet());
        authUser.setResourceCodeSet(resourceCodeSet);

        AuthUtil.saveTokenToAuthUserMap(login.getToken(), authUser);
        return authUser;
    }

    @Override
    public void logout() {
        AuthUtil.logout();
    }
}
