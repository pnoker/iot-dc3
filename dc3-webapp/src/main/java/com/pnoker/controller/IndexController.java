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
package com.pnoker.controller;

import com.alibaba.fastjson.JSON;
import com.pnoker.rtmp.feign.RtmpFeignApi;
import com.pnoker.common.base.BaseController;
import com.pnoker.common.model.rtmp.Rtmp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Rest接口控制器
 */
@Slf4j
@RestController
public class IndexController extends BaseController {
    @Autowired
    private RtmpFeignApi rtmpFeignApi;

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String hello() {
        log.info("hello world");
        String wrapper = rtmpFeignApi.api();
        List<Rtmp> rtmp = rtmpFeignApi.list();
        log.info(JSON.toJSONString(wrapper));
        log.info(JSON.toJSONString(rtmp));
        return JSON.toJSONString(wrapper);
    }

}
