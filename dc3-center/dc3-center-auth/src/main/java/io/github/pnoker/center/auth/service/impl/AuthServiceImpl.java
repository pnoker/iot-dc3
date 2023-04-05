package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.center.auth.service.*;
import io.github.pnoker.center.auth.utils.UserRedisUtil;
import io.github.pnoker.common.constant.cache.TimeoutConstant;
import io.github.pnoker.common.constant.common.SuffixConstant;
import io.github.pnoker.common.entity.auth.Login;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.*;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.KeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

    @Resource
    private UserRedisUtil userRedisUtil;

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
        String redisSaltKey = userRedisUtil.getKey(SuffixConstant.SALT, login.getName(), tenant.getId());
        String redisSaltValue = userRedisUtil.getValue(redisSaltKey);
        String decodePassword = DecodeUtil.md5(userPassword.getLoginPassword() + redisSaltValue);
        if (CharSequenceUtil.isNotEmpty(redisSaltValue) && redisSaltValue.equals(login.getSalt())
                && decodePassword.equals(login.getPassword())) {
            //generate token
            String token = KeyUtil.generateToken(login.getName(), redisSaltValue, tenant.getId());
            String redisTokenKey = userRedisUtil.getKey(SuffixConstant.TOKEN, login.getName(), tenant.getId());
            userRedisUtil.setKey(redisTokenKey, token, TimeoutConstant.TOKEN_CACHE_TIMEOUT, TimeUnit.HOURS);
            login.setToken(token);
            return userLogin;
        }

        //todo updateUserLimit
        return null;
    }

    @Override
    public String login(Login login) {
        //1. authenticate user
        UserLogin userLogin = authenticateUser(login);
        if (ObjectUtil.isNull(userLogin)) {
            throw new ServiceException("认证失败！请重试");
        }

        //2. save roles
        Tenant tenant = tenantService.selectByCode(login.getTenant());
        List<Role> roles = roleUserBindService.listRoleByTenantIdAndUserId(tenant.getId(), userLogin.getUserId());
        if (CollUtil.isEmpty(roles)) {
            throw new ServiceException("请先为用户{}分配角色", login.getName());
        }
        Set<String> roleCodeSet = roles.stream().map(Role::getRoleCode).collect(Collectors.toSet());
        String redisRoleKey = userRedisUtil.getKey(SuffixConstant.ROLE, login.getName(), tenant.getId());
        userRedisUtil.setSetValue(redisRoleKey, roleCodeSet, TimeoutConstant.TOKEN_CACHE_TIMEOUT, TimeUnit.HOURS);

        //3. save resources
        Set<io.github.pnoker.common.model.Resource> resourceSet = new HashSet<>();
        for (Role role : roles) {
            List<io.github.pnoker.common.model.Resource> resources = roleResourceBindService.listResourceByRoleId(role.getId());
            resourceSet.addAll(resources);
        }
        if (CollUtil.isEmpty(resourceSet)) {
            throw new ServiceException("请先为用户{}分配权限", login.getName());
        }
        Set<String> resourceCodeSet = resourceSet.stream().map(io.github.pnoker.common.model.Resource::getResourceCode).collect(Collectors.toSet());
        String redisResourceKey = userRedisUtil.getKey(SuffixConstant.RESOURCE, login.getName(), tenant.getId());
        userRedisUtil.setSetValue(redisResourceKey, resourceCodeSet, TimeoutConstant.TOKEN_CACHE_TIMEOUT, TimeUnit.HOURS);

        return login.getToken();
    }
}
