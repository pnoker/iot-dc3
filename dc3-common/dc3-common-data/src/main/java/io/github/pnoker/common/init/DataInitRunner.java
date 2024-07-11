/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.common.init;

import io.github.pnoker.common.data.biz.ScheduleService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Data initialization runner
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Component
@EnableTransactionManagement
@ComponentScan(basePackages = {
        "io.github.pnoker.common.data.*"
})
@MapperScan("io.github.pnoker.common.data.mapper")
public class DataInitRunner implements ApplicationRunner {

    private final ScheduleService scheduleService;

    public DataInitRunner(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        scheduleService.initial();
    }
}
