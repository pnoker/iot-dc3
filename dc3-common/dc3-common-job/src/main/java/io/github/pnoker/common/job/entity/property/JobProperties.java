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

package io.github.pnoker.common.job.entity.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "xxl.job")
public class JobProperties {

    /**
     * Xxl Job admin server addresses
     */
    private String adminAddresses;

    /**
     * Xxl Job access token
     */
    private String accessToken;

    /**
     * Xxl Job executor app name
     */
    private String appName;

    /**
     * Xxl Job address
     */
    private String address;

    /**
     * Xxl Job ip
     */
    private String ip;

    /**
     * Xxl Job executor port
     */
    private int executorPort;

    /**
     * Xxl Job log retention path
     */
    private String logPath;

    /**
     * Xxl Job log retention days
     */
    private int logRetentionDays;
}
