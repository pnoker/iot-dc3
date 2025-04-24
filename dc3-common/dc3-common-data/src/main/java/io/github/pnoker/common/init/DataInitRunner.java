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

import io.github.pnoker.common.data.biz.ScheduleForDataService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * Data initialization runner
 *
 * @author pnoker
 * @version 2025.2.4
 * @since 2022.1.0
 */
@Component
@ComponentScan(basePackages = {
        "io.github.pnoker.common.data.*"
})
@MapperScan(basePackages = {
        "io.github.pnoker.common.data.mapper"
})
public class DataInitRunner implements ApplicationRunner {

    private final ScheduleForDataService scheduleForDataService;

    public DataInitRunner(ScheduleForDataService scheduleForDataService) {
        this.scheduleForDataService = scheduleForDataService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        scheduleForDataService.initial();
    }
}
