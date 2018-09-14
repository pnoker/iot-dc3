package com.pnoker.service.dbs.feign;

import com.pnoker.api.dbs.service.UserFeignApi;
import com.pnoker.common.core.support.BaseController;
import com.pnoker.common.wrapper.WrapMapper;
import com.pnoker.common.wrapper.Wrapper;
import com.pnoker.service.dbs.model.User;
import com.pnoker.service.dbs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Wrapper<User> getById(Long userId) {
        logger.info("search userId {}", userId);
        User user = userService.selectByKey(userId);
        return WrapMapper.ok(user);
    }
}
