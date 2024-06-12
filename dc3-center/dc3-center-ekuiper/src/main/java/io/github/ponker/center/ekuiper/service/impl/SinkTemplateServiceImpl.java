package io.github.ponker.center.ekuiper.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ponker.center.ekuiper.constant.EkuiperConstant;
import io.github.ponker.center.ekuiper.entity.dto.*;
import io.github.ponker.center.ekuiper.service.SinkTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author : Zhen
 * @date : 2024/3/12
 */
@Slf4j
@Service
public class SinkTemplateServiceImpl implements SinkTemplateService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Mono<String> callApiSinktem(HttpMethod method, String url, Object data, String name) {
        WebClient.RequestBodySpec request = webClient.method(method).uri(url);
        try {
            String dataJsonBody = objectMapper.writeValueAsString(data);
            JsonNode dataJsonNode = objectMapper.readTree(dataJsonBody);

            switch (name) {
                case io.github.ponker.center.ekuiper.constant.EkuiperConstant.ConKeyType.MQTT:
                    handleMqttRequest(request, dataJsonNode, dataJsonBody);
                    break;
                case io.github.ponker.center.ekuiper.constant.EkuiperConstant.ConKeyType.REST:
                    handleRestRequest(request, dataJsonNode, dataJsonBody);
                    break;
                case io.github.ponker.center.ekuiper.constant.EkuiperConstant.ConKeyType.SQL:
                    handleSqlRequest(request, dataJsonNode, dataJsonBody);
                    break;
                case io.github.ponker.center.ekuiper.constant.EkuiperConstant.ConKeyType.REDIS:
                    handleRedisRequest(request, dataJsonNode, dataJsonBody);
                    break;
                case EkuiperConstant.ConKeyType.WEBSOCKET:
                    handleWebSocketRequest(request, dataJsonNode, dataJsonBody);
                    break;
                default:
                    throw new IllegalArgumentException("无效的名称");
            }
        } catch (JsonProcessingException e) {
            log.info("将对象序列化为JSON失败");
            e.printStackTrace();
        }
        return request.retrieve()
                //.bodyToMono
//                .toBodilessEntity().map(responseEntity -> responseEntity.getStatusCode().is2xxSuccessful());
                //.bodyToMono(String.class)
                .toBodilessEntity()
                .flatMap(responseEntity -> {
                    if (responseEntity.getStatusCode().is2xxSuccessful()) {
                        return Mono.just("true"); // 返回成功消息 "true"
                    } else {
                        return Mono.error(new RuntimeException("请求失败--状态码错误: " + responseEntity.getStatusCode()));
                    }
                })
                .onErrorResume(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException responseException = (WebClientResponseException) error;
                        log.error("调用callApiSinktem时发生错误. Status code: {}, Response body: {}",
                                responseException.getRawStatusCode(), responseException.getResponseBodyAsString());
                        return Mono.error(new RuntimeException(responseException.getResponseBodyAsString()));
                    } else {
                        log.error("调用callApiSinktem时发生错误: {}", error.getMessage());
                        return Mono.error(error);
                    }
                });
    }

    @Override
    public Mono<String> callConfigAndSink(HttpMethod method, String url) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        // 解析原始 JSON
                        JsonNode root = objectMapper.readTree(response);

                        // 构造新的 JSON 结构
                        ArrayNode newSinks = objectMapper.createArrayNode();
                        ArrayNode newSources = objectMapper.createArrayNode();

                        // 处理 sinks
                        JsonNode sinks = root.get("sinks");
                        for (JsonNode sinkGroup : sinks) {
                            Iterator<Map.Entry<String, JsonNode>> fields = sinkGroup.fields();
                            while (fields.hasNext()) {
                                Map.Entry<String, JsonNode> entry = fields.next();
                                ObjectNode newSink = objectMapper.createObjectNode();
                                newSink.put("name", entry.getKey());
                                newSink.put("type", entry.getValue().asText());
                                newSinks.add(newSink);
                            }
                        }

                        // 处理 sources
                        JsonNode sources = root.get("sources");
                        for (JsonNode sourceGroup : sources) {
                            Iterator<Map.Entry<String, JsonNode>> fields = sourceGroup.fields();
                            while (fields.hasNext()) {
                                Map.Entry<String, JsonNode> entry = fields.next();
                                ObjectNode newSource = objectMapper.createObjectNode();
                                newSource.put("name", entry.getKey());
                                newSource.put("type", entry.getValue().asText());
                                newSources.add(newSource);
                            }
                        }

                        // 构造新的 JSON 结构
                        ObjectNode newRoot = objectMapper.createObjectNode();
                        newRoot.set("sinks", newSinks);
                        newRoot.set("sources", newSources);

                        // 将修改后的 JSON 结构转换为字符串
                        return objectMapper.writeValueAsString(newRoot);
                    } catch (Exception e) {
                        log.error("处理JSON时发生错误: {}", e.getMessage());
                        throw new RuntimeException("处理JSON时发生错误", e);
                    }
                })
                .onErrorResume(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException responseException = (WebClientResponseException) error;
                        log.error("调用callConfigAndSink时发生错误. Status code: {}, Response body: {}",
                                responseException.getRawStatusCode(), responseException.getResponseBodyAsString());
                        return Mono.error(new RuntimeException(responseException.getResponseBodyAsString()));
                    } else {
                        log.error("调用callConfigAndSink时发生错误: {}", error.getMessage());
                        return Mono.error(error);
                    }
                });
    }

    private void handleMqttRequest(WebClient.RequestBodySpec request, JsonNode dataJsonNode, String dataJsonBody)  {
        try {
            MqttSinkDto mqttSinkDto = new MqttSinkDto();
            JsonNode mqttJsonNode = generateJsonNode(mqttSinkDto);

            if (containsAllFields(dataJsonNode, mqttJsonNode)) {
                request = (WebClient.RequestBodySpec) request.body(BodyInserters.fromValue(dataJsonBody));
            } else {
                log.info("The data does not contain all fields from mqttSink");
                throw new IllegalArgumentException("The data does not contain all fields from mqttSink");
            }

        } catch (Exception e) {
            log.info("Failed to serialize object to JSON:");
            e.printStackTrace();
        }
    }

    private void handleRestRequest(WebClient.RequestBodySpec request, JsonNode dataJsonNode,String dataJsonBody)  {
        try {
            RestSinkDto restSinkDto = new RestSinkDto();
            JsonNode httppushJsonNode = generateJsonNode(restSinkDto);
            if (containsAllFields(dataJsonNode, httppushJsonNode)) {
                request = (WebClient.RequestBodySpec) request.body(BodyInserters.fromValue(dataJsonBody));
            } else {
                log.info("The data does not contain all fields from httpPushConfigForm");
                throw new IllegalArgumentException("The data does not contain all fields from httpPushConfigForm");
            }

        } catch (Exception e) {
            log.info("Failed to serialize object to JSON:");
            e.printStackTrace();
        }
    }

    private void handleSqlRequest(WebClient.RequestBodySpec request, JsonNode dataJsonNode,String dataJsonBody)  {
        try {
            SqlSinkDto sqlSinkDto = new SqlSinkDto();
            JsonNode sqlJsonNode = generateJsonNode(sqlSinkDto);

            if (containsAllFields(dataJsonNode, sqlJsonNode)) {
                request = (WebClient.RequestBodySpec) request.body(BodyInserters.fromValue(dataJsonBody));
            } else {
                log.info("The data does not contain all fields from sqlConfigForm");
                throw new IllegalArgumentException("The data does not contain all fields from sqlConfigForm");
            }

        } catch (Exception e) {
            log.info("Failed to serialize object to JSON:");
            e.printStackTrace();
        }
    }

    private void handleRedisRequest(WebClient.RequestBodySpec request, JsonNode dataJsonNode,String dataJsonBody)  {
        try {
            RedisSinkDto redisSinkDto = new RedisSinkDto();
            JsonNode redisJsonNode = generateJsonNode(redisSinkDto);
            if (containsAllFields(dataJsonNode, redisJsonNode)) {
                request = (WebClient.RequestBodySpec) request.body(BodyInserters.fromValue(dataJsonBody));
            } else {
                log.info("The data does not contain all fields from redisConfigForm");
                throw new IllegalArgumentException("The data does not contain all fields from redisConfigForm");
            }

        } catch (Exception e) {
            log.info("Failed to serialize object to JSON:");
            e.printStackTrace();
        }
    }

    private void handleWebSocketRequest(WebClient.RequestBodySpec request, JsonNode dataJsonNode,String dataJsonBody)  {
        try {
            WebSocketSinkDto webSocketSinkDto = new WebSocketSinkDto();
            JsonNode webSocketJsonNode = generateJsonNode(webSocketSinkDto);
            if (containsAllFields(dataJsonNode, webSocketJsonNode)) {
                request = (WebClient.RequestBodySpec) request.body(BodyInserters.fromValue(dataJsonBody));
            } else {
                log.info("The data does not contain all fields from  webSocketSinkForm");
                throw new IllegalArgumentException("The data does not contain all fields from webSocketSinkForm");
            }

        } catch (Exception e) {
            log.info("Failed to serialize object to JSON:");
            e.printStackTrace();
        }
    }

    private boolean containsAllFields(JsonNode node1, JsonNode node2) {
        Set<String> keys1 = new HashSet<>();
        Set<String> keys2 = new HashSet<>();

        node1.fieldNames().forEachRemaining(keys1::add);
        node2.fieldNames().forEachRemaining(keys2::add);
        return keys1.equals(keys2);
    }

    private JsonNode generateJsonNode(Object data) throws JsonProcessingException {

        String dataJsonBody = objectMapper.writeValueAsString(data);
        JsonNode dataJsonNode = objectMapper.readTree(dataJsonBody);
        return dataJsonNode;
    }

}
