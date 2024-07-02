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

package io.github.pnoker.common.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import io.github.pnoker.common.job.entity.property.JobProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XxlJob配置
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Configuration
public class JobConfig {

    private final JobProperties jobProperties;

    public JobConfig(JobProperties jobProperties) {
        this.jobProperties = jobProperties;
    }

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(jobProperties.getAdminAddresses());
        xxlJobSpringExecutor.setAppname(jobProperties.getAppName());
        xxlJobSpringExecutor.setAddress(jobProperties.getAddress());
        xxlJobSpringExecutor.setIp(jobProperties.getIp());
        xxlJobSpringExecutor.setPort(jobProperties.getExecutorPort());
        xxlJobSpringExecutor.setAccessToken(jobProperties.getAccessToken());
        xxlJobSpringExecutor.setLogPath(jobProperties.getLogPath());
        xxlJobSpringExecutor.setLogRetentionDays(jobProperties.getLogRetentionDays());
        return xxlJobSpringExecutor;
    }

}