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

package io.github.pnoker.center.auth.config;

import io.github.pnoker.common.utils.OpenAPIUtil;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Config
 * <p>
 * 请勿移动到 common web，否则无法生效
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI openAPI() {
        return OpenAPIUtil.getOpenAPI("权限服务接口文档");
    }
}
