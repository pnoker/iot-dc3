/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.github.pnoker.common.sdk.init;

import com.github.pnoker.common.sdk.service.DriverCommonService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 初始化
 *
 * @author pnoker
 */
@Component
@EnableFeignClients(basePackages = {
        "com.github.pnoker.api.center.manager.*"
})
@ComponentScan(basePackages = {
        "com.github.pnoker.api.center.manager",
        "com.github.pnoker.common.sdk"
})
public class DriverInitRunner implements ApplicationRunner {
    @Resource
    private DriverCommonService driverCommonService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        driverCommonService.initial();
    }
}
