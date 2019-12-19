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

package com.pnoker.auth.schedule;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.auth.service.TokenAuthService;
import com.pnoker.auth.service.UserAuthService;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.bean.Response;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.Token;
import com.pnoker.common.entity.auth.User;
import com.pnoker.common.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>定时刷新用户 Token
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Component
public class TokenSchedule {
    @Resource
    private TokenAuthService tokenAuthService;
    @Resource
    private UserAuthService userAuthService;

    @Resource
    private RedisUtil redisUtil;

    /**
     * 单页查询用户个数
     */
    private int pageSize = 2;
    /**
     * 缓存时间 5 分钟
     */
    private int expireTime = 5;

    @Scheduled(cron = "0/10 * * * * *")
    public void refreshToken() {
        Response<Page<User>> response = userAuthService.list(new UserDto().setEnable(true).setPage(new Pages().setPageNum(1).setPageSize(pageSize)));
        if (response.isOk()) {
            long pages = response.getData().getPages();
            if (pages == 1) {
                writeTokenCache(response.getData().getRecords());
            } else if (pages > 1) {
                for (int page = 2; page <= pages; page++) {
                    List<User> userList = userList(page);
                    writeTokenCache(userList);
                }
            }
        }
    }

    /**
     * 获取分页用户列表
     *
     * @param page
     * @return
     */
    public List<User> userList(int page) {
        UserDto userDto = new UserDto().setEnable(true).setPage(new Pages().setPageNum(page).setPageSize(pageSize));
        Response<Page<User>> response = userAuthService.list(userDto);
        if (response.isOk()) {
            return response.getData().getRecords();
        }
        return null;
    }

    /**
     * 将 Token 写入缓存 (key=userId,value=token)
     *
     * @param userList
     */
    public void writeTokenCache(List<User> userList) {
        for (User user : userList) {
            Response<Token> tokenResponse = tokenAuthService.selectById(user.getTokenId());
            if (tokenResponse.isOk()) {
                Token token = tokenResponse.getData();
                redisUtil.setKey(user.getId().toString(), token, expireTime, TimeUnit.MINUTES);
            }
        }
    }

}
