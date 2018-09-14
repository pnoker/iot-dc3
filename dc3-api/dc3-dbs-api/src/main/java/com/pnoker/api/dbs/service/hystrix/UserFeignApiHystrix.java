package com.pnoker.api.dbs.service.hystrix;

import com.pnoker.api.dbs.service.UserFeignApi;
import com.pnoker.common.wrapper.Wrapper;
import com.pnoker.service.dbs.model.User;
import org.springframework.stereotype.Component;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: iot-dc3
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>
 */
@Component
public class UserFeignApiHystrix implements UserFeignApi {
    @Override
    public Wrapper<User> getById(Long userId) {
        return null;
    }
}
