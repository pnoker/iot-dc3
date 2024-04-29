package io.github.ponker.center.ekuiper.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ponker.center.ekuiper.entity.dto.*;
import io.github.ponker.center.ekuiper.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

/**
 * @author : Zhen
 * @date : 2024/3/11
 */
@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SqlConfigDto SqlConfigDto;


    @Override
    public Mono<String> callApiDelConf(HttpMethod method, String url) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                //.bodyToMono(String.class);
                //.toBodilessEntity().map(responseEntity -> responseEntity.getStatusCode().is2xxSuccessful());
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
                        log.error("调用callApiDelConf时发生错误. Status code: {}, Response body: {}",
                                responseException.getRawStatusCode(), responseException.getResponseBodyAsString());
                        return Mono.error(new RuntimeException(responseException.getResponseBodyAsString()));
                    } else {
                        log.error("调用callApiDelConf时发生错误: {}", error.getMessage());
                        return Mono.error(error);
                    }
                });
    }

    @Override
    public Mono<String> callApiConfig(HttpMethod method, String url, Object data, String name) {
        WebClient.RequestBodySpec request = webClient.method(method).uri(url);
        try {
            String dataJsonBody = objectMapper.writeValueAsString(data);
            JsonNode dataJsonNode = objectMapper.readTree(dataJsonBody);

            switch (name) {
                case io.github.ponker.center.ekuiper.constant.EkuiperConstant.ConKeyType.MQTT:
                    handleMqttRequest(request, dataJsonNode, dataJsonBody);
                    break;
                case io.github.ponker.center.ekuiper.constant.EkuiperConstant.ConKeyType.HTTPPUSH:
                    handleHttpPushRequest(request, dataJsonNode, dataJsonBody);
                    break;
                case io.github.ponker.center.ekuiper.constant.EkuiperConstant.ConKeyType.SQL:
                    handleSqlRequest(request, dataJsonNode, dataJsonBody);
                    break;
                case io.github.ponker.center.ekuiper.constant.EkuiperConstant.ConKeyType.HTTPPULL:
                    handleHttpPullRequest(request, dataJsonNode, dataJsonBody);
                    break;
                case io.github.ponker.center.ekuiper.constant.EkuiperConstant.ConKeyType.REDIS:
                    handleRedisRequest(request, dataJsonNode, dataJsonBody);
                    break;
                default:
                    throw new IllegalArgumentException("无效的名称");
            }
        } catch (JsonProcessingException e) {
            log.info("对象序列化为JSON失败");
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
                        log.error("调用callApiConfig时发生错误. Status code: {}, Response body: {}",
                                responseException.getRawStatusCode(), responseException.getResponseBodyAsString());
                        return Mono.error(new RuntimeException(responseException.getResponseBodyAsString()));
                    } else {
                        log.error("调用callApiConfig时发生错误: {}", error.getMessage());
                        return Mono.error(error);
                    }
                });
    }

    private void handleMqttRequest(WebClient.RequestBodySpec request, JsonNode dataJsonNode, String dataJsonBody) {
        try {
            MqttConfigDto mqttConfigDto = new MqttConfigDto();
            JsonNode mqttJsonNode = generateJsonNode(mqttConfigDto);

            if (containsAllFields(dataJsonNode, mqttJsonNode)) {
                request = (WebClient.RequestBodySpec) request.body(BodyInserters.fromValue(dataJsonBody));
            } else {
                log.info("The data does not contain all fields from mqttConfigForm");
                throw new IllegalArgumentException("The data does not contain all fields from mqttConfigForm");
            }

        } catch (Exception e) {
            log.info("将对象序列化为JSON失败");
            e.printStackTrace();
        }
    }

    private void handleHttpPushRequest(WebClient.RequestBodySpec request, JsonNode dataJsonNode, String dataJsonBody) {
        try {
            HttppushConfigDto httppushConfigDto = new HttppushConfigDto();
            JsonNode httppushJsonNode = generateJsonNode(httppushConfigDto);
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

    private void handleSqlRequest(WebClient.RequestBodySpec request, JsonNode dataJsonNode, String dataJsonBody) {
        try {
            //SqlConfigDto sqlConfigDto = new SqlConfigDto();
            JsonNode sqlJsonNode = generateJsonNode(SqlConfigDto);

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

    private void handleHttpPullRequest(WebClient.RequestBodySpec request, JsonNode dataJsonNode, String dataJsonBody) {
        try {
            HttppullConfigDto httppullConfigDto = new HttppullConfigDto();
            JsonNode httppulJsonNode = generateJsonNode(httppullConfigDto);
            if (containsAllFields(dataJsonNode, httppulJsonNode)) {
                request = (WebClient.RequestBodySpec) request.body(BodyInserters.fromValue(dataJsonBody));
            } else {
                log.info("The data does not contain all fields from httpPullConfigForm");
                throw new IllegalArgumentException("The data does not contain all fields from httpPullConfigForm");
            }

        } catch (Exception e) {
            log.info("Failed to serialize object to JSON:");
            e.printStackTrace();
        }
    }

    private void handleRedisRequest(WebClient.RequestBodySpec request, JsonNode dataJsonNode, String dataJsonBody) {
        try {
            RedisConfigDto redisConfigDto = new RedisConfigDto();
            JsonNode redisJsonNode = generateJsonNode(redisConfigDto);
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
