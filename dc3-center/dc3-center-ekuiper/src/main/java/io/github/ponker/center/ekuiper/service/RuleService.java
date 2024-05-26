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
import io.github.ponker.center.ekuiper.entity.vo.DetailRuleVO;
import io.github.ponker.center.ekuiper.entity.vo.RuleDataVO;
import io.github.ponker.center.ekuiper.entity.vo.RuleStatusVO;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author : Zhen
 * @date : 2024/3/11
 */
public interface RuleService {

    Mono<DetailRuleVO> callApilRule(HttpMethod method, String url);

    Mono<RuleStatusVO> callApiRuleStatus(HttpMethod method, String url);

    Mono<String> callRuleApiWithData(Object data, HttpMethod method, String url) throws Exception;

    Mono<Page<RuleDataVO>> callApiWithRulePage(HttpMethod method, String url, Integer current, Integer size);

    Mono<String> addActions(Object data);

    Mono<List<Map<String, Map<String, Object>>>> listActions();

    Mono<String> deleteActions(String actionType);

    Mono<String> deleteActions(String actionType, Integer index);

    Mono<String> deleteActions(Integer index);

    Mono<String> editActions(Object data, String type);

    Mono<String> editActions(Object data, Integer index);

    Mono<String> editActions(Object data, String type, Integer index);


    Mono<String> deleteAllActions();

}
