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
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ponker.center.ekuiper.entity.dto.MqttSinkDto;
import io.github.ponker.center.ekuiper.entity.dto.RedisSinkDto;
import io.github.ponker.center.ekuiper.entity.dto.RestSinkDto;
import io.github.ponker.center.ekuiper.entity.dto.SqlSinkDto;
import io.github.ponker.center.ekuiper.entity.vo.DetailRuleVO;
import io.github.ponker.center.ekuiper.entity.vo.RuleDataVO;
import io.github.ponker.center.ekuiper.entity.vo.RuleStatusVO;
import io.github.ponker.center.ekuiper.exception.EkuiperException;
import io.github.ponker.center.ekuiper.service.RuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;


/**
 * @author : Zhen
 * @date : 2024/3/11
 */
@Slf4j
@Service
public class RuleServiceImpl implements RuleService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;
    private List<Map<String, Map<String, Object>>> actions = new ArrayList<>();

    public static Page<RuleDataVO> getPageSubset2(int current, int size, List<RuleDataVO> ruleDataVOList) {
        int fromIndex = Math.max((current - 1) * size, 0);
        int toIndex = Math.min(current * size, ruleDataVOList.size());
        List<RuleDataVO> subset = ruleDataVOList.subList(fromIndex, toIndex);
        Page<RuleDataVO> page = new Page<>(current, size, ruleDataVOList.size());
        page.setRecords(subset);
        return page;
    }

    @Override
    public Mono<DetailRuleVO> callApilRule(HttpMethod method, String url) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(DetailRuleVO.class);
    }

    @Override
    public Mono<RuleStatusVO> callApiRuleStatus(HttpMethod method, String url) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(RuleStatusVO.class);
    }

    @Override
    public Mono<String> callRuleApiWithData(Object data, HttpMethod method, String url) throws Exception {
        WebClient.RequestBodySpec request = webClient.method(method).uri(url);
        if (Objects.nonNull(data)) {
            try {

                String jsonBody = objectMapper.writeValueAsString(data);
                JsonNode dataJsonNode = generateJsonNode(data);
                validateSinkStructure(dataJsonNode);

                request = (WebClient.RequestBodySpec) request.body(BodyInserters.fromValue(jsonBody));
            } catch (JsonProcessingException e) {
                log.error("将对象序列化为JSON失败: {}", e.getMessage());
                throw new EkuiperException("JSON序列化失败");
            }
        }
        return request.retrieve()
                .bodyToMono(String.class)
                .onErrorResume(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException responseException = (WebClientResponseException) error;
                        log.error("调用callRuleApiWithData时发生错误. Status code: {}, Response body: {}",
                                responseException.getRawStatusCode(), responseException.getResponseBodyAsString());
                        return Mono.error(new RuntimeException(responseException.getResponseBodyAsString()));
                    } else {
                        log.error("调用callRuleApiWithData时发生错误: {}", error.getMessage());
                        return Mono.error(error);
                    }
                });
    }

    @Override
    public Mono<Page<RuleDataVO>> callApiWithRulePage(HttpMethod method, String url, Integer current, Integer size) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    //获取响应后进行MyBatisPlus分页查询
                    List<RuleDataVO> ruleDataVOList = getDataList2(response);
                    Page<RuleDataVO> page = getPageSubset2(current, size, ruleDataVOList);
                    return Mono.just(page);
                });
    }

    @Override
    public Mono<String> addActions(Object data) {
        try {
            String jsonBody = objectMapper.writeValueAsString(data);
            JsonNode dataJsonNode = generateJsonNode(data);
            //validateActionNodes(dataJsonNode);

        } catch (Exception e) {
            log.error("将对象序列化为JSON失败: {}", e.getMessage());
            throw new EkuiperException("JSON序列化失败");
        }

        //actions.clear(); // 清空 actions 集合
        actions.add((Map<String, Map<String, Object>>) data); // 添加新元素
        return Mono.just("successful");
    }

    public Mono<List<Map<String, Map<String, Object>>>> listActions() {

        return Mono.just(actions);
    }

    public Mono<String> deleteActions(String actionType) {
        try {
            // 遍历 actions 列表
            Iterator<Map<String, Map<String, Object>>> iterator = actions.iterator();
            while (iterator.hasNext()) {
                Map<String, Map<String, Object>> action = iterator.next();
                // 如果 action 包含指定的 actionType, 则删除
                if (action.containsKey(actionType)) {
                    iterator.remove();
                }
            }
            // 操作完成后返回 "successful"
            return Mono.just("successful");
        } catch (Exception e) {
            // 如果发生异常, 则返回 Mono.error
            return Mono.error(e);
        }
    }

    @Override
    public Mono<String> deleteActions(String actionType, Integer index) {
        try {
            int currentIndex = 0; // 当前数据索引
            // 遍历 actions 列表
            Iterator<Map<String, Map<String, Object>>> iterator = actions.iterator();
            while (iterator.hasNext()) {
                Map<String, Map<String, Object>> action = iterator.next();
                // 检查当前 action 是否包含指定的 actionType
                if (action.containsKey(actionType)) {
                    // 如果索引等于指定的 index, 则删除
                    if (currentIndex == index) {
                        iterator.remove();
                        return Mono.just("删除成功");
                    }
                    currentIndex++; // 增加索引
                }
            }
            // 如果没有找到匹配的数据, 则返回错误信息
            return Mono.error(new RuntimeException("找不到类型为: " + actionType + ", 索引为: " + index + " 的数据"));
        } catch (Exception e) {
            // 如果发生异常, 则返回 Mono.error
            return Mono.error(e);
        }
    }

    @Override
    public Mono<String> deleteActions(Integer index) {
        try {
            int currentIndex = 0; // 当前数据索引
            // 遍历 actions 列表
            Iterator<Map<String, Map<String, Object>>> iterator = actions.iterator();
            while (iterator.hasNext()) {
                Map<String, Map<String, Object>> action = iterator.next();
                // 如果当前 action 不为空
                if (!action.isEmpty()) {
                    // 如果索引等于指定的 index, 则删除
                    if (currentIndex == index) {
                        iterator.remove();
                        return Mono.just("删除成功");
                    }
                    currentIndex++; // 增加索引
                }
            }
            // 如果没有找到匹配的数据, 则返回错误信息
            return Mono.error(new RuntimeException("索引超出范围"));
        } catch (Exception e) {
            // 如果发生异常, 则返回 Mono.error
            return Mono.error(e);
        }
    }

    @Override
    public Mono<String> editActions(Object data, String type) {
        // 解析传入的数据为Map对象
        Map<String, Map<String, Object>> newData = (Map<String, Map<String, Object>>) data;

        // 遍历actions列表中的每个Map对象
        for (Map<String, Map<String, Object>> actionMap : actions) {
            // 获取指定类型的结构
            Map<String, Object> targetConfig = actionMap.get(type);

            // 如果存在指定类型的结构, 则更新配置
            if (targetConfig != null) {
                updateConfig(targetConfig, newData.get(type));
                return Mono.just(type + "编辑成功");
            }
        }

        // 如果actions列表中没有指定类型的结构, 则返回错误信息
        return Mono.error(new RuntimeException("No configuration found for type: " + type));
    }

    @Override
    public Mono<String> editActions(Object data, String type, Integer index) {
        // 解析传入的数据为Map对象
        Map<String, Map<String, Object>> newData = (Map<String, Map<String, Object>>) data;

        int currentIndex = 0; // 当前 MQTT 数据索引

        // 遍历 actions 列表中的每个 Map 对象
        for (Map<String, Map<String, Object>> actionMap : actions) {
            // 检查当前 Map 对象是否包含指定类型的结构
            if (actionMap.containsKey(type)) {
                // 如果是 MQTT 类型, 并且索引等于指定的 mqttIndex, 则更新配置并返回成功消息
                if (currentIndex == index) {
                    updateConfig(actionMap.get(type), newData.get(type));
                    return Mono.just(type + "[" + index + "]" + "编辑成功");
                }
                currentIndex++; // 仅当类型为 MQTT 时增加索引
            }
        }

        // 如果 actions 列表中没有指定类型和索引的结构, 则返回错误信息
        return Mono.error(new RuntimeException("找不到类型为: " + type + ", 索引为: " + index + " 的配置"));
    }

    @Override
    public Mono<String> editActions(Object data, Integer index) {
        // 解析传入的数据为Map<String, Map<String, Object>>对象
        Map<String, Map<String, Object>> newDataMap = (Map<String, Map<String, Object>>) data;

        // 检查 index 是否在有效范围内
        if (index >= 0 && index < actions.size()) {

            Map<String, Map<String, Object>> newData = newDataMap;

            // 遍历 actions 列表中的每个 Map 对象
            for (int i = 0; i < actions.size(); i++) {
                // 检查当前 Map 对象是否与 newData 相等
                if (actions.get(index) != null) {
                    // 转换原始配置和新配置为Map<String, Object>类型
                    Map<String, Object> originalConfig = convertToMap(actions.get(i));
                    Map<String, Object> newConfig = convertToMap(newData);
                    // 调用updateConfig方法更新配置
                    updateConfig(originalConfig, newConfig);
                    // 更新actions列表中的数据
                    actions.set(index, newData);
                    return Mono.just("actions中第" + (index + 1) + "个sink编辑成功");
                }
            }

            // 如果 actions 列表中没有与 newData 匹配的结构, 则返回错误信息
            return Mono.error(new RuntimeException("找不到要编辑的数据"));
        } else {
            return Mono.error(new RuntimeException("索引越界"));
        }
    }

    // 更新配置方法
    private void updateConfig(Map<String, Object> originalConfig, Map<String, Object> newConfig) {
        for (Map.Entry<String, Object> entry : newConfig.entrySet()) {
            // 检查新传入的字段是否有更新
            if (originalConfig.containsKey(entry.getKey())) {
                // 如果新传入的字段在原始配置中存在, 则更新原始配置中的对应字段
                originalConfig.put(entry.getKey(), entry.getValue());
            }
        }
    }

    // 辅助方法: 将Map<String, Map<String, Object>>类型转换为Map<String, Object>类型
    private Map<String, Object> convertToMap(Map<String, Map<String, Object>> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    public Mono<String> deleteAllActions() {
        actions.clear(); // 清空 actions 集合
        return Mono.just("successful");
    }

    private JsonNode generateJsonNode(Object data) throws JsonProcessingException {
        String dataJsonBody = objectMapper.writeValueAsString(data);
        JsonNode dataJsonNode = objectMapper.readTree(dataJsonBody);
        return dataJsonNode;
    }

    private boolean containsAllFields(JsonNode node1, JsonNode node2) {
        Set<String> keys1 = new HashSet<>();
        Set<String> keys2 = new HashSet<>();
        node1.fieldNames().forEachRemaining(keys1::add);
        node2.fieldNames().forEachRemaining(keys2::add);
        return keys1.equals(keys2);
    }

    private List<RuleDataVO> getDataList2(String response) {
        List<RuleDataVO> ruleDataVOList = new ArrayList<>();
        try {
            // 解析成 JSON 数组
            JSONArray jsonArray = JSONArray.parseArray(response);
            // 遍历 JSON 数组中的每个元素, 将其转换为 Data 对象, 并添加到 dataList 中
            for (int i = 0; i < jsonArray.size(); i++) {
                // 获取 JSON 对象
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                // 解析 JSON 对象的属性
                String id = jsonObject.getString("id");
                String name = jsonObject.getString("name");
                String status = jsonObject.getString("status");
                // 创建 Data 对象, 并添加到 dataList 中
                RuleDataVO data = new RuleDataVO(id, name, status);
                ruleDataVOList.add(data);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ruleDataVOList;
    }

    private void validateSinkStructure(JsonNode dataJsonNode) throws Exception {
        if (!dataJsonNode.has("actions") || !dataJsonNode.has("id") || !dataJsonNode.has("sql")) {
            log.error("RuleSink结构错误");
            throw new EkuiperException("RuleSink结构错误");
        }
        JsonNode actionsNode = dataJsonNode.get("actions");
        if (!actionsNode.isArray()) {
            log.error("actionsNode结构错误");
            throw new EkuiperException("actionsNode结构错误");
        }
        for (JsonNode action : actionsNode) {
            String actionName = action.fieldNames().next();
            log.info("发现action: " + actionName);

            JsonNode actionNode = action.get(actionName);
            if (!isValidSinkType(actionName, actionNode)) {
                log.info("Sink中{}表单结构不符合规范", actionName.toUpperCase());
                throw new EkuiperException("Sink中" + actionName.toUpperCase() + "表单结构不符合规范");
            }
        }
    }

    private void validateActionNodes(JsonNode dataJsonNode) throws Exception {
        if (!dataJsonNode.has("actions")) {
            log.error("actionNodes结构错误");
            throw new EkuiperException("actionNodes结构错误");
        }
        JsonNode actionsNode = dataJsonNode.get("actions");
        if (!actionsNode.isArray()) {
            log.error("actionsNode结构错误");
            throw new EkuiperException("actionsNode结构错误");
        }
        for (JsonNode action : actionsNode) {
            String actionName = action.fieldNames().next();
            log.info("发现action: " + actionName);

            JsonNode actionNode = action.get(actionName);
            if (!isValidSinkType(actionName, actionNode)) {
                log.info("Sink中{}表单结构不符合规范", actionName.toUpperCase());
                throw new EkuiperException("Sink中" + actionName.toUpperCase() + "表单结构不符合规范");
            }
        }
    }

    private boolean isValidSinkType(String actionName, JsonNode actionNode) throws JsonProcessingException {

        switch (actionName) {
            case "mqtt":
                return containsAllFields(generateJsonNode(new MqttSinkDto()), actionNode);
            case "redis":
                return containsAllFields(generateJsonNode(new RedisSinkDto()), actionNode);
            case "rest":
                return containsAllFields(generateJsonNode(new RestSinkDto()), actionNode);
            case "sql":
                return containsAllFields(generateJsonNode(new SqlSinkDto()), actionNode);
            default:
                return false;
        }
    }

}
