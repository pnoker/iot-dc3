/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.driver.controller;

import io.github.pnoker.driver.server.Lwm2mServer;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 提供部分测试接口,方便驱动测试
 * tips:需要依赖spring-boot-starter-web
 */
@RestController
@RequestMapping("/api")
public class ServerApiController {

    @Resource
    Lwm2mServer lwm2mServer;

    @GetMapping("/{name}")
    public String read(@PathVariable(value = "name") String clientName, String path) {
        return lwm2mServer.readValueByPath(clientName, path);
    }

    @PutMapping("/{name}")
    public Boolean write(@PathVariable(value = "name") String clientName, String path, String value) {
        return lwm2mServer.writeValueByPath(clientName, path, value, false);
    }

    @PostMapping("/{name}")
    public boolean obs(@PathVariable(value = "name") String clientName, String path) {
        return lwm2mServer.observe(clientName, path);
    }

    @DeleteMapping("/{name}")
    public void cancel(@PathVariable(value = "name") String clientName, String path) {
        lwm2mServer.cancelObs(clientName, path);
    }
}
