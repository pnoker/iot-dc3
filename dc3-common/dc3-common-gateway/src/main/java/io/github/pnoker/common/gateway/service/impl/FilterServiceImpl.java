/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.gateway.service.impl;

import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.api.TokenFacade;
import io.github.pnoker.common.facade.api.UserFacade;
import io.github.pnoker.common.facade.api.UserLoginFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.facade.entity.bo.FacadeUserLoginBO;
import io.github.pnoker.common.gateway.service.FilterService;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RequestUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class FilterServiceImpl implements FilterService {

    @Resource
    private TenantFacade tenantFacade;

    @Resource
    private UserLoginFacade userLoginFacade;

    @Resource
    private UserFacade userFacade;

    @Resource
    private TokenFacade tokenFacade;

    @Override
    public FacadeTenantBO getTenant(ServerHttpRequest request) {
        String code = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TENANT);
        if (StringUtils.isEmpty(code)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        FacadeTenantBO tenant = tenantFacade.selectByCode(code);
        if (Objects.isNull(tenant) || tenant.getEnableFlag() != EnableFlagEnum.ENABLE) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        return tenant;
    }

    @Override
    public FacadeUserLoginBO getUserLogin(ServerHttpRequest request) {
        String name = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_LOGIN);
        if (StringUtils.isEmpty(name)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        FacadeUserLoginBO userLogin = userLoginFacade.selectByName(name);
        if (Objects.isNull(userLogin) || userLogin.getEnableFlag() != EnableFlagEnum.ENABLE) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        return userLogin;
    }

    @Override
    public RequestHeader.UserHeader getUser(FacadeUserLoginBO userLogin, FacadeTenantBO tenant) {
        // Preserves the existing (surprising) behavior: lookup UserApi by UserLogin.id,
        // not UserLogin.userId. Changing that belongs in a separate bug-fix PR.
        FacadeUserBO user = userFacade.selectById(userLogin.getId());
        if (Objects.isNull(user)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        RequestHeader.UserHeader header = new RequestHeader.UserHeader();
        header.setUserId(user.getId());
        header.setNickName(user.getNickName());
        header.setUserName(user.getUserName());
        header.setTenantId(tenant.getId());
        return header;
    }

    @Override
    public void checkValid(ServerHttpRequest request, FacadeTenantBO tenant, FacadeUserLoginBO userLogin) {
        String token = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TOKEN);
        RequestHeader.TokenHeader header;
        try {
            header = JsonUtil.parseObject(token, RequestHeader.TokenHeader.class);
        } catch (Exception e) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        if (Objects.isNull(header) || StringUtils.isAnyEmpty(header.getSalt(), header.getToken())) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        boolean valid = tokenFacade.checkValid(tenant.getTenantCode(), userLogin.getLoginName(), header.getSalt(),
                header.getToken());
        if (!valid) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
    }

}
