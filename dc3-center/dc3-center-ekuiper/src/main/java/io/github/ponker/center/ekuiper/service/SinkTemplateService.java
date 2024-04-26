package io.github.ponker.center.ekuiper.service;

import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

/**
 * @author : Zhen
 * @date : 2024/3/12
 */
public interface SinkTemplateService {

    Mono<String> callApiSinktem (HttpMethod method, String url, Object data, String name);

    Mono<String> callConfigAndSink(HttpMethod method, String url);

}
