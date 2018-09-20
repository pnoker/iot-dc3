package com.pnoker.service.dbs.controller;

import com.alibaba.fastjson.JSON;
import com.pnoker.api.dbs.UserFeignApi;
import com.pnoker.common.util.core.support.BaseController;
import com.pnoker.common.util.model.domain.User;
import com.pnoker.common.util.wrapper.WrapMapper;
import com.pnoker.common.util.wrapper.Wrapper;
import com.pnoker.service.dbs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: iot-dc3
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>
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
