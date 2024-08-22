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

package io.github.ponker.center.ekuiper.service.impl;

import io.github.ponker.center.ekuiper.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author : Zhen
 * @date : 2024/2/20
 */

@Slf4j
@Service
public class UrlServiceImpl implements UrlService {

    @Value("${driver.custom.STREAM_URL}")
    private String streamUrl;

    @Value("${driver.custom.TABLE_URL}")
    private String tableUrl;

    @Value("${driver.custom.CONFKEY_URL}")
    private String confKeyUrl;

    @Value("${driver.custom.RULE_URL}")
    private String ruleUrl;
    @Value("${driver.custom.PORTABLE_URL}")
    private String portableUrl;
    @Value("${driver.custom.UPLOAD_URL}")
    private String uploadFileUrl;
    @Override
    public String getStreamUrl() {
        return streamUrl;
    }

    @Override
    public String getTableUrl() {
        return tableUrl;
    }

    @Override
    public String getConfigUrl() {
        return confKeyUrl;
    }

    @Override
    public String getRuleUrl() {
        return ruleUrl;
    }

    @Override
    public String getPortableUrl() {
        return portableUrl;
    }

    @Override
    public String getUploadFileUrl() {
        return uploadFileUrl;
    }
}
