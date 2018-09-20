package com.pnoker.controller;

import com.alibaba.fastjson.JSON;
import com.pnoker.api.dbs.UserFeignApi;
import com.pnoker.common.util.core.support.BaseController;
import com.pnoker.common.util.wrapper.Wrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Pnoker
 * @email : pnokers@gmail.com
 * <p>
 * Rest接口控制器
 */
@RestController
public class IndexController extends BaseController {
    @Autowired
    private UserFeignApi userFeignApi;

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String hello() {
        logger.info("hello world");
        Wrapper<String> wrapper = userFeignApi.getById(12L);
        logger.info(JSON.toJSONString(wrapper));
        return JSON.toJSONString(wrapper);
    }

}
