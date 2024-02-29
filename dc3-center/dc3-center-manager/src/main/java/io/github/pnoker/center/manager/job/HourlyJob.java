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

package io.github.pnoker.center.manager.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 通用：每小时执行任务
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class HourlyJob {

    /**
     * 任务执行
     * <p>
     * 具体逻辑请在 biz service 中定义
     *
     * @throws Exception 异常
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyJobHandler() throws Exception {
        log.info("hourlyJobHandler:{}", LocalDateTime.now());
    }
}
