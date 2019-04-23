/*
 * Copyright 2018 Google LLC. All Rights Reserved.
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
package com.pnoker.dbs.controller;

import com.alibaba.fastjson.JSON;
import com.pnoker.api.dbs.UserFeignApi;
import com.pnoker.common.base.BaseController;
import com.pnoker.common.util.model.domain.User;
import com.pnoker.common.wrapper.WrapMapper;
import com.pnoker.common.wrapper.Wrapper;
import com.pnoker.dbs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@RestController
public class UserFeignClient extends BaseController implements UserFeignApi {
    @Autowired
    private UserService userService;


    @Override
    public Wrapper<String> getById(@PathVariable("userId") Long userId) {
        logger.info("search userId {}", userId);
        User user = userService.selectByKey(userId);
        return WrapMapper.wrap(Wrapper.SUCCESS_CODE, Wrapper.SUCCESS_MESSAGE, JSON.toJSONString(user));
    }
}
