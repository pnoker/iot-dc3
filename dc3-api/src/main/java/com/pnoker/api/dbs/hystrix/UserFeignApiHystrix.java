package com.pnoker.api.dbs.hystrix;

import com.pnoker.api.dbs.UserFeignApi;
import com.pnoker.common.util.wrapper.WrapMapper;
import com.pnoker.common.util.wrapper.Wrapper;
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
    public Wrapper<String> getById(Long userId) {
        return WrapMapper.wrap(Wrapper.ERROR_CODE,Wrapper.ERROR_MESSAGE);
    }
}
