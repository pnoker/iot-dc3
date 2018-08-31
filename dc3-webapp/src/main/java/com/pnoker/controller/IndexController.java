package com.pnoker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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
    RestTemplate restTemplate;

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String hello() {
        return restTemplate.getForEntity("http://DC3-DBS/hello", String.class).getBody();
    }
}
