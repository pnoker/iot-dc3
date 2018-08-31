package com.pnoker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Pnoker
 * @email : pnokers@gmail.com
 * <p>
 * Rest接口控制器
 */
@Slf4j
@RestController
public class IndexController {
    @Autowired
    private Environment environment;

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String helloController() {
        return String.format("hello world with %s", environment.getProperty("server.port"));
    }
}
