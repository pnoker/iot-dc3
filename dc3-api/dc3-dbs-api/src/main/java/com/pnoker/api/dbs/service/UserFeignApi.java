package com.pnoker.api.dbs.service;

import com.pnoker.api.dbs.service.hystrix.UserFeignApiHystrix;
import com.pnoker.common.wrapper.Wrapper;
import com.pnoker.service.dbs.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: iot-dc3
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>
 */
@FeignClient(value = "dc3-service-dbs", fallback = UserFeignApiHystrix.class)
public interface UserFeignApi {
    @PostMapping(value = "/api/user/getById/{userId}")
    Wrapper<User> getById(@PathVariable("userId") Long userId);
}
