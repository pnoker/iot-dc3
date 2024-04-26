package io.github.ponker.center.ekuiper.service;

import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

/**
 * @author : Zhen
 * @date : 2024/3/11
 */
public interface ConfigService {


    Mono<String> callApiDelConf(HttpMethod method, String url);


    Mono<String> callApiConfig (HttpMethod method, String url, Object data, String name);
}
