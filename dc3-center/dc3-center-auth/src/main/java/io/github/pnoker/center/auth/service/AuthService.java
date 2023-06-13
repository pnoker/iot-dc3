package io.github.pnoker.center.auth.service;

import io.github.pnoker.common.entity.auth.Login;
import io.github.pnoker.common.model.AuthUser;
import io.github.pnoker.common.model.UserLogin;

/**
 * User Manage Service
 *
 * @author: linys
 * @since: 2023.04.02
 */
public interface AuthService {

    /**
     * 鉴定用户, 并返回token
     *
     * @param login login info
     * @return UserLogin userLogin
     */
    UserLogin authenticateUser(Login login);

    /**
     * 用户登录
     *
     * @param login 登录参数
     * @return AuthUser
     */
    AuthUser login(Login login);

    /**
     * 当前用户退出登录
     */
    void logout();
}
