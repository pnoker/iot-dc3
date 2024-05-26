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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ponker.center.ekuiper.entity.dto.RecordDto;
import io.github.ponker.center.ekuiper.service.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author : Zhen
 * @date : 2024/3/11
 */
@Slf4j
@Service
public class ApiServiceImpl implements ApiService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Mono<String> callApi(HttpMethod method, String url) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException responseException = (WebClientResponseException) error;
                        log.error("调用API时发生错误. Status code: {}, Response body: {}",
                                responseException.getRawStatusCode(), responseException.getResponseBodyAsString());
                        return Mono.error(new RuntimeException(responseException.getResponseBodyAsString()));
                    } else {
                        log.error("调用API时发生错误: {}", error.getMessage());
                        return Mono.error(error);
                    }
                });
    }

    @Override
    public Mono<String> callApiWithData(Object data, HttpMethod method, String url) {
        WebClient.RequestBodySpec request = webClient.method(method).uri(url);
        if (Objects.nonNull(data)) {
            try {
                String jsonBody = objectMapper.writeValueAsString(data);
                request = (WebClient.RequestBodySpec) request.body(BodyInserters.fromValue(jsonBody));

            } catch (Exception e) {
                log.info("将对象序列化为JSON失败");
                return Mono.error(e);
            }
        }
        return request
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException responseException = (WebClientResponseException) error;
                        log.error("调用callApiWithData时发生错误. Status code: {}, Response body: {}",
                                responseException.getRawStatusCode(), responseException.getResponseBodyAsString());
                        return Mono.error(new RuntimeException(responseException.getResponseBodyAsString()));
                    } else {
                        log.error("调用callApiWithData时发生错误: {}", error.getMessage());
                        return Mono.error(error);
                    }

                });
    }

    @Override
    public Mono<Page<RecordDto>> callApiWithPage(HttpMethod method, String url, Integer current, Integer size) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    //获取响应后进行MyBatisPlus分页查询
                    List<RecordDto> dataList = getDataList(response); // 修改此处
                    Page<RecordDto> page = getPageSubset(current, size, dataList); // 修改此处
                    return Mono.just(page);
                });
    }

    private List<RecordDto> getDataList(String response) {
        List<RecordDto> dataList = new ArrayList<>();
        try {
            // 解析成JSON数组
            JSONArray jsonArray = JSONArray.parseArray(response);
            // 将 JSON 数组中的元素逐个添加到 dataList 中
            for (int i = 0; i < jsonArray.size(); i++) {
                String recordName = jsonArray.getString(i);
                dataList.add(new RecordDto(recordName));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return dataList;
    }

    private Page<RecordDto> getPageSubset(int current, int size, List<RecordDto> dataList) {
        int fromIndex = Math.max((current - 1) * size, 0);
        int toIndex = Math.min(current * size, dataList.size());
        List<RecordDto> subset = dataList.subList(fromIndex, toIndex);
        Page<RecordDto> page = new Page<>(current, size, dataList.size());
        page.setRecords(subset);
        return page;
    }

}
