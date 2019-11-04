/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.common.security.service;

import com.pnoker.api.dbs.user.feign.UserDbsFeignApi;
import com.pnoker.common.constant.CommonConstants;
import com.pnoker.common.model.User;
import com.pnoker.common.utils.Response;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>用户详细信息
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Service
@AllArgsConstructor
public class Dc3UserDetailsServiceImpl implements UserDetailsService {
    private final UserDbsFeignApi remoteUserService;
    private final CacheManager cacheManager;

    /**
     * 用户密码登录
     *
     * @param username 用户名
     * @return
     */
    @Override
    @SneakyThrows
    public UserDetails loadUserByUsername(String username) {
        Cache cache = cacheManager.getCache("user_details");
        if (cache != null && cache.get(username) != null) {
            return (Dc3User) cache.get(username).get();
        }

        Response<User> result = remoteUserService.user(username);
        UserDetails userDetails = getUserDetails(result);
        cache.put(username, userDetails);
        return userDetails;
    }

    /**
     * 构建userdetails
     *
     * @param result 用户信息
     * @return
     */
    private UserDetails getUserDetails(Response<User> result) {
        if (result == null || result.getData() == null) {
            throw new UsernameNotFoundException("user does not exist");
        }

        User info = result.getData();
        Set<String> dbAuthsSet = new HashSet<>();
        Collection<? extends GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(dbAuthsSet.toArray(new String[0]));

        // 构造security用户
        return new Dc3User(info.getId(), info.getUsername(), CommonConstants.BCRYPT + info.getPassword(), authorities);
    }
}
