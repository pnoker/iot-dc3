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

import io.github.ponker.center.ekuiper.entity.vo.DetailStreamVO;
import io.github.ponker.center.ekuiper.entity.vo.DetailTableVO;
import io.github.ponker.center.ekuiper.service.StreamTableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author : Zhen
 * @date : 2024/3/11
 */
@Slf4j
@Service
public class StreamTableServiceImpl implements StreamTableService {

    @Autowired
    private WebClient webClient;

    @Override
    public Mono<DetailStreamVO> callApiStream(HttpMethod method, String url) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(DetailStreamVO.class);
    }

    @Override
    public Mono<DetailTableVO> callApiTable(HttpMethod method, String url) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(DetailTableVO.class);
    }
}
