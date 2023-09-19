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

package io.github.pnoker.gateway.filter.factory;

import io.github.pnoker.gateway.filter.AuthenticGatewayFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * 自定义 Request Header 校验过滤器工厂
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class AuthenticGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Resource
    private AuthenticGatewayFilter authenticGatewayFilter;

    @Override
    public GatewayFilter apply(Object config) {
        return authenticGatewayFilter;
    }

}
