package com.pnoker.api.dbs;

import com.pnoker.api.dbs.hystrix.UserFeignApiHystrix;
import com.pnoker.common.util.wrapper.Wrapper;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: iot-dc3
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>
 */
@FeignClient(name = "DC3-SERVICE-DBS", fallback = UserFeignApiHystrix.class)
public interface UserFeignApi {
    @RequestMapping(value = "/api/user/getById/{userId}", method = RequestMethod.GET)
    Wrapper<String> getById(@PathVariable("userId") Long userId);
}
