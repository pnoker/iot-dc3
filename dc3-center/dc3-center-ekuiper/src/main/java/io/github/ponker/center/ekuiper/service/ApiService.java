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

package io.github.ponker.center.ekuiper.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.ponker.center.ekuiper.entity.dto.RecordDto;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

/**
 * @author : Zhen
 * @date : 2024/3/11
 */

public interface ApiService {


    Mono<String> callApi(HttpMethod method, String url);


    Mono<String> callApiWithData(Object data, HttpMethod method, String url);


    Mono<Page<RecordDto>> callApiWithPage(HttpMethod method, String url, Integer current, Integer size);

    Mono<String> callApiWithFile(Mono<FilePart> uploadFile, HttpMethod post, String url);

    Mono<String> callApiWithMuFile(MultipartFile uploadFile, HttpMethod post, String url);
}
