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

package io.github.ponker.center.ekuiper.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.ponker.center.ekuiper.constant.ServiceConstant;
import io.github.ponker.center.ekuiper.entity.R;
import io.github.ponker.center.ekuiper.entity.dto.RecordDto;
import io.github.ponker.center.ekuiper.entity.dto.StreamFormDto;
import io.github.ponker.center.ekuiper.entity.vo.DetailStreamVO;
import io.github.ponker.center.ekuiper.service.ApiService;
import io.github.ponker.center.ekuiper.service.StreamTableService;
import io.github.ponker.center.ekuiper.service.UrlService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @author : Zhen
 */

@Slf4j
@RestController
@RequestMapping(ServiceConstant.Ekuiper.STREAM_URL_PREFIX)
public class StreamController {

    @Resource
    private ApiService apiService;

    @Resource
    private StreamTableService streamTableService;

    @Resource
    private UrlService urlService;

    /**
     * 创建流
     */
    @PostMapping("/create")
    public Mono<R<String>> createStream(@Validated @RequestBody StreamFormDto form) {
        Mono<String> stringMono = apiService.callApiWithData(form, HttpMethod.POST, urlService.getStreamUrl());
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call create Stream API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 显示所有流
     */
    @GetMapping("/list")
    public Mono<R<Page<RecordDto>>> listStream(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                               @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        Mono<Page<RecordDto>> pageMono = apiService.callApiWithPage(HttpMethod.GET, urlService.getStreamUrl(), pageNum, pageSize);
        return pageMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call list Stream API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 删除流
     */
    @DeleteMapping("/delete")
    public Mono<R<String>> deleteStream(@RequestParam(name = "id") String id) {
        String url = urlService.getStreamUrl() + "/" + id;
        Mono<String> stringMono = apiService.callApi(HttpMethod.DELETE, url);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call delete Stream API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 描述流
     */
    @GetMapping("/detail")
    public Mono<R<DetailStreamVO>> detailStream(@RequestParam(name = "id") String id) {
        String url = urlService.getStreamUrl() + "/" + id;
        Mono<DetailStreamVO> detailStreamMono = streamTableService.callApiStream(HttpMethod.GET, url);
        return detailStreamMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s, "successful"));
            } catch (Exception e) {
                log.error("Failed to call detail Stream API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 更新流
     */
    @PutMapping("/update")
    public Mono<R<String>> updateStream(@Validated @RequestBody StreamFormDto form, @RequestParam(name = "id") String id) {
        String url = urlService.getStreamUrl() + "/" + id;
        Mono<String> stringMono = apiService.callApiWithData(form, HttpMethod.PUT, url);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call update Stream API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }
}
